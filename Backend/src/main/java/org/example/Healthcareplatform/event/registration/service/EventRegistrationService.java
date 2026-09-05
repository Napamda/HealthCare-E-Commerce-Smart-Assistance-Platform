package org.example.Healthcareplatform.event.registration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.example.Healthcareplatform.event.entity.EventStatus;
import org.example.Healthcareplatform.event.entity.HealthEvent;
import org.example.Healthcareplatform.event.registration.dto.EventRegistrationResponse;
import org.example.Healthcareplatform.event.registration.dto.EventRegistrationStatusResponse;
import org.example.Healthcareplatform.event.registration.entity.EventRegistration;
import org.example.Healthcareplatform.event.registration.entity.RegistrationStatus;
import org.example.Healthcareplatform.event.registration.repository.EventRegistrationRepository;
import org.example.Healthcareplatform.event.repository.HealthEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventRegistrationService {

    private final EventRegistrationRepository registrationRepository;
    private final HealthEventRepository eventRepository;
    private final SecurityContextUtil securityContextUtil;

    @Transactional
    public EventRegistrationResponse register(Long eventId) {
        Long userId = securityContextUtil.getCurrentUserId();
        HealthEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalArgumentException("Event is not open for registration");
        }
        if (event.getStartDateTime() != null && event.getStartDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Event has already started");
        }
        // Doctors and pharmacists register as volunteers; their professional role
        // is stored so it can be surfaced as a badge in the UI.
        String volunteerRole = resolveVolunteerRole();

        EventRegistration existing = registrationRepository.findByEventIdAndUserId(eventId, userId).orElse(null);
        if (existing != null && existing.getStatus() == RegistrationStatus.CONFIRMED) {
            throw new IllegalArgumentException("You are already registered for this event");
        }
        long confirmedCount = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
        if (event.getCapacity() != null && confirmedCount >= event.getCapacity()) {
            throw new IllegalArgumentException("Event is full");
        }
        EventRegistration registration;
        if (existing != null) {
            existing.setStatus(RegistrationStatus.CONFIRMED);
            existing.setVolunteerRole(volunteerRole);
            existing.setUpdatedAt(LocalDateTime.now());
            registration = registrationRepository.save(existing);
        } else {
            registration = registrationRepository.save(EventRegistration.builder()
                    .event(event)
                    .userId(userId)
                    .status(RegistrationStatus.CONFIRMED)
                    .volunteerRole(volunteerRole)
                    .build());
        }
        log.info("Event registered — eventId={}, userId={}, volunteerRole={}", eventId, userId, volunteerRole);
        return toResponse(registration);
    }

    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> myRegistrations() {
        Long userId = securityContextUtil.getCurrentUserId();
        return registrationRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, RegistrationStatus.CONFIRMED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void cancel(Long registrationId) {
        Long userId = securityContextUtil.getCurrentUserId();
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        if (!registration.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only cancel your own registrations");
        }
        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setUpdatedAt(LocalDateTime.now());
        registrationRepository.save(registration);
        log.info("Event registration cancelled — id={}, userId={}", registrationId, userId);
    }

    @Transactional(readOnly = true)
    public EventRegistrationStatusResponse status(Long eventId) {
        Long userId = securityContextUtil.getCurrentUserId();
        HealthEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));
        long count = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
        EventRegistration existing = registrationRepository.findByEventIdAndUserId(eventId, userId).orElse(null);
        boolean registered = existing != null && existing.getStatus() == RegistrationStatus.CONFIRMED;
        return EventRegistrationStatusResponse.builder()
                .count(count)
                .capacity(event.getCapacity())
                .registered(registered)
                .registrationId(registered ? existing.getId() : null)
                .volunteerRole(registered ? existing.getVolunteerRole() : null)
                .build();
    }

    /**
     * Returns the current user's professional role (DOCTOR or PHARMACIST) when
     * they should be flagged as a volunteer at registration time. Other roles
     * (PATIENT, VENDOR, ADMIN) return null.
     */
    private String resolveVolunteerRole() {
        String role = securityContextUtil.getCurrentUserRole();
        if ("DOCTOR".equals(role) || "PHARMACIST".equals(role)) {
            return role;
        }
        return null;
    }

    private EventRegistrationResponse toResponse(EventRegistration registration) {
        HealthEvent event = registration.getEvent();
        return EventRegistrationResponse.builder()
                .id(registration.getId())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .category(event.getCategory())
                .startDateTime(event.getStartDateTime())
                .venue(event.getVenue())
                .city(event.getCity())
                .organizer(event.getOrganizer())
                .status(registration.getStatus())
                .registeredAt(registration.getCreatedAt())
                .volunteerRole(registration.getVolunteerRole())
                .build();
    }
}
