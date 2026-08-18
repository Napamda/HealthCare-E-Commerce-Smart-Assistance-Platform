package org.example.Healthcareplatform.event.service;

import lombok.RequiredArgsConstructor;
import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.example.Healthcareplatform.event.dto.HealthEventRequest;
import org.example.Healthcareplatform.event.dto.HealthEventResponse;
import org.example.Healthcareplatform.event.entity.EventStatus;
import org.example.Healthcareplatform.event.entity.HealthEvent;
import org.example.Healthcareplatform.event.repository.HealthEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HealthEventService {

    private final HealthEventRepository eventRepository;
    private final SecurityContextUtil securityContextUtil;

    @Transactional(readOnly = true)
    public List<HealthEventResponse> listEvents(String category, String status) {
        List<HealthEvent> events;
        EventStatus parsedStatus = parseStatus(status);

        if (parsedStatus != null && category != null && !category.isBlank()) {
            events = eventRepository.findByStatusAndCategoryIgnoreCaseOrderByStartDateTimeAsc(parsedStatus, category);
        } else if (parsedStatus != null) {
            events = eventRepository.findByStatusOrderByStartDateTimeAsc(parsedStatus);
        } else if (category != null && !category.isBlank()) {
            events = eventRepository.findByCategoryIgnoreCaseOrderByStartDateTimeAsc(category);
        } else {
            events = eventRepository.findAllByOrderByStartDateTimeAsc();
        }
        return events.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return eventRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public HealthEventResponse getEvent(Long id) {
        return toResponse(findEvent(id));
    }

    @Transactional
    public HealthEventResponse createEvent(HealthEventRequest request) {
        validate(request);
        HealthEvent event = HealthEvent.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .category(request.getCategory().trim())
                .tags(normalizeTags(request.getTags()))
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .venue(request.getVenue())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .organizer(request.getOrganizer())
                .capacity(request.getCapacity())
                .status(request.getStatus() == null ? EventStatus.PUBLISHED : request.getStatus())
                .createdBy(securityContextUtil.getCurrentUserId())
                .build();
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public HealthEventResponse updateEvent(Long id, HealthEventRequest request) {
        validate(request);
        HealthEvent event = findEvent(id);
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory().trim());
        event.setTags(normalizeTags(request.getTags()));
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setVenue(request.getVenue());
        event.setCity(request.getCity());
        event.setLatitude(request.getLatitude());
        event.setLongitude(request.getLongitude());
        event.setOrganizer(request.getOrganizer());
        event.setCapacity(request.getCapacity());
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(Long id) {
        HealthEvent event = findEvent(id);
        eventRepository.delete(event);
    }

    private HealthEvent findEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + id));
    }

    private void validate(HealthEventRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Event title is required");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new IllegalArgumentException("Event category is required");
        }
        if (request.getStartDateTime() == null) {
            throw new IllegalArgumentException("Event start date is required");
        }
        if (request.getEndDateTime() != null && request.getEndDateTime().isBefore(request.getStartDateTime())) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    private Set<String> normalizeTags(List<String> tags) {
        Set<String> normalized = new LinkedHashSet<>();
        if (tags == null) {
            return normalized;
        }
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                normalized.add(tag.trim());
            }
        }
        return normalized;
    }

    private EventStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return EventStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid event status: " + status);
        }
    }

    private HealthEventResponse toResponse(HealthEvent event) {
        return HealthEventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .tags(new ArrayList<>(event.getTags()))
                .startDateTime(event.getStartDateTime())
                .endDateTime(event.getEndDateTime())
                .venue(event.getVenue())
                .city(event.getCity())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .organizer(event.getOrganizer())
                .capacity(event.getCapacity())
                .status(event.getStatus())
                .createdBy(event.getCreatedBy())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
