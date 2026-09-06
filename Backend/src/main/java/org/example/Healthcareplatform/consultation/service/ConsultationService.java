package org.example.Healthcareplatform.consultation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.example.Healthcareplatform.ai.service.ConversationService;
import org.example.Healthcareplatform.consultation.dto.ConsultationResponse;
import org.example.Healthcareplatform.consultation.dto.EscalationRequest;
import org.example.Healthcareplatform.consultation.entity.Consultation;
import org.example.Healthcareplatform.consultation.event.ConsultationCreatedEvent;
import org.example.Healthcareplatform.consultation.exception.ConsultationConflictException;
import org.example.Healthcareplatform.consultation.repository.ConsultationRepository;
import org.example.Healthcareplatform.messaging.publisher.HealthcareEventPublisher;
import org.example.Healthcareplatform.notification.entity.Notification;
import org.example.Healthcareplatform.notification.service.NotificationService;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NotificationService notificationService;
    private final HealthcareEventPublisher eventPublisher;

    @Transactional
    public ConsultationResponse escalateFromChat(EscalationRequest request, Long patientUserId) {
        if (consultationRepository.existsByConversationIdAndStatusNot(
                request.getConversationId(), Consultation.ConsultationStatus.CLOSED)) {
            throw new IllegalArgumentException(
                    "An active consultation already exists for conversation " + request.getConversationId());
        }

        List<ConversationMessage> messages = conversationService.getMessages(request.getConversationId());
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("Cannot escalate an empty conversation");
        }

        String chatContext = serializeChatContext(messages);

        Consultation.Priority priority = resolvePriority(request.getPriority());

        Consultation consultation = Consultation.builder()
                .conversationId(request.getConversationId())
                .patientUserId(patientUserId)
                .status(Consultation.ConsultationStatus.PENDING)
                .priority(priority)
                .reason(request.getReason())
                .chatContext(chatContext)
                .build();

        Consultation saved = consultationRepository.save(consultation);
        log.info("Consultation created — id={}, conversationId={}, patientUserId={}, priority={}",
                saved.getId(), saved.getConversationId(), saved.getPatientUserId(), saved.getPriority());

        applicationEventPublisher.publishEvent(ConsultationCreatedEvent.builder()
                .consultationId(saved.getId())
                .conversationId(saved.getConversationId())
                .patientUserId(saved.getPatientUserId())
                .reason(saved.getReason())
                .priority(saved.getPriority().name())
                .createdAt(Instant.now())
                .build());

        return toResponse(saved, messages);
    }

    public ConsultationResponse getConsultation(Long consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found: " + consultationId));

        List<ConversationMessage> messages = conversationService.getMessages(consultation.getConversationId());
        return toResponse(consultation, messages);
    }

    public List<ConsultationResponse> getPatientConsultations(Long patientUserId) {
        return consultationRepository.findByPatientUserIdOrderByCreatedAtDesc(patientUserId)
                .stream()
                .map(c -> toResponse(c, conversationService.getMessages(c.getConversationId())))
                .toList();
    }

    @Transactional
    public ConsultationResponse updateStatus(Long consultationId, String newStatus, Long doctorUserId,
                                             String rejectionReason, Instant scheduledAt) {
        // Row lock: serializes concurrent accept attempts on the same request.
        Consultation consultation = consultationRepository.findByIdForUpdate(consultationId)
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found: " + consultationId));

        Consultation.ConsultationStatus target = parseStatus(newStatus);
        Consultation.ConsultationStatus current = consultation.getStatus();
        Long ownerId = consultation.getDoctorUserId();

        switch (target) {
            case ACCEPTED -> {
                if (current == Consultation.ConsultationStatus.PENDING && ownerId == null) {
                    // Claim an unclaimed request (atomic under the row lock).
                    consultation.setDoctorUserId(doctorUserId);
                    consultation.setStatus(Consultation.ConsultationStatus.ACCEPTED);
                    if (scheduledAt != null) {
                        consultation.setScheduledAt(scheduledAt);
                    }
                } else if (current == Consultation.ConsultationStatus.ACCEPTED
                        && ownerId != null && doctorUserId.equals(ownerId) && scheduledAt != null) {
                    // Reschedule: the assigned doctor moves an accepted appointment.
                    consultation.setScheduledAt(scheduledAt);
                } else {
                    throw new ConsultationConflictException(
                            "This consultation was already claimed by another doctor.");
                }
            }
            case IN_PROGRESS -> {
                requireOwner(ownerId, doctorUserId, consultationId);
                if (current != Consultation.ConsultationStatus.ACCEPTED) {
                    throw new IllegalArgumentException(
                            "Cannot start a consultation in status " + current);
                }
                consultation.setStatus(Consultation.ConsultationStatus.IN_PROGRESS);
            }
            case CLOSED -> {
                requireOwner(ownerId, doctorUserId, consultationId);
                if (current != Consultation.ConsultationStatus.ACCEPTED
                        && current != Consultation.ConsultationStatus.IN_PROGRESS) {
                    throw new IllegalArgumentException(
                            "Cannot close a consultation in status " + current);
                }
                consultation.setStatus(Consultation.ConsultationStatus.CLOSED);
            }
            case REJECTED -> {
                requireOwner(ownerId, doctorUserId, consultationId);
                if (current != Consultation.ConsultationStatus.ACCEPTED
                        && current != Consultation.ConsultationStatus.IN_PROGRESS) {
                    throw new IllegalArgumentException(
                            "Cannot reject a consultation in status " + current);
                }
                consultation.setStatus(Consultation.ConsultationStatus.REJECTED);
                if (rejectionReason != null && !rejectionReason.isBlank()) {
                    consultation.setRejectionReason(rejectionReason);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported status: " + newStatus);
        }

        Consultation saved = consultationRepository.save(consultation);
        log.info("Consultation id={} status updated to {} by doctorUserId={}",
                saved.getId(), saved.getStatus(), doctorUserId);

        // Notify the patient when a doctor accepts or starts the consultation.
        // (Rescheduling an already-accepted appointment keeps the status and
        // therefore must not re-notify or re-publish.)
        Consultation.ConsultationStatus status = saved.getStatus();
        boolean actuallyTransitioned = current != target;
        if (actuallyTransitioned
                && (status == Consultation.ConsultationStatus.ACCEPTED
                || status == Consultation.ConsultationStatus.IN_PROGRESS)) {
            String doctorName = doctorUserId != null
                    ? userRepository.findById(doctorUserId)
                            .map(u -> u.getFirstName() + " " + u.getLastName())
                            .orElse("a doctor")
                    : "a doctor";
            String title = status == Consultation.ConsultationStatus.ACCEPTED
                    ? "Consultation Accepted"
                    : "Consultation In Progress";
            String message = String.format(
                    "Dr. %s has %s your consultation. Open your chat to continue the conversation.",
                    doctorName,
                    status == Consultation.ConsultationStatus.ACCEPTED ? "accepted" : "started");
            Notification.NotificationType type = status == Consultation.ConsultationStatus.ACCEPTED
                    ? Notification.NotificationType.CONSULTATION_ACCEPTED
                    : Notification.NotificationType.CONSULTATION_IN_PROGRESS;
            try {
                notificationService.createNotification(
                        saved.getPatientUserId(),
                        title,
                        message,
                        type,
                        saved.getId());
            } catch (Exception e) {
                log.warn("Failed to send consultation notification to patient userId={}: {}",
                        saved.getPatientUserId(), e.getMessage());
            }

            // Publish consultation.scheduled event to RabbitMQ
            String patientEmail = userRepository.findById(saved.getPatientUserId())
                    .map(User::getEmail).orElse("");
            String patientName = userRepository.findById(saved.getPatientUserId())
                    .map(u -> u.getFirstName() + " " + u.getLastName()).orElse("");
            eventPublisher.publishConsultationScheduled(
                    saved.getId(), saved.getPatientUserId(), patientEmail, patientName,
                    doctorUserId, doctorName);
        }

        return toResponse(saved);
    }

    public List<ConsultationResponse> getDoctorQueue(Long doctorUserId) {
        // Shared pool of unclaimed requests + this doctor's own active cases.
        // No doctor sees another doctor's accepted/in-progress consultations.
        List<Consultation> pool = consultationRepository
                .findByDoctorUserIdIsNullAndStatusOrderByPriorityAscCreatedAtAsc(
                        Consultation.ConsultationStatus.PENDING);
        List<Consultation> mine = consultationRepository
                .findByDoctorUserIdAndStatusInOrderByPriorityAscCreatedAtAsc(
                        doctorUserId,
                        List.of(Consultation.ConsultationStatus.ACCEPTED,
                                Consultation.ConsultationStatus.IN_PROGRESS));
        List<Consultation> queue = new ArrayList<>(pool);
        queue.addAll(mine);
        return queue.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ConsultationResponse> getDoctorUpcoming(Long doctorUserId) {
        List<Consultation.ConsultationStatus> upcomingStatuses = List.of(
                Consultation.ConsultationStatus.ACCEPTED,
                Consultation.ConsultationStatus.IN_PROGRESS);
        return consultationRepository
                .findByStatusInAndDoctorUserIdOrderByScheduledAtAscCreatedAtDesc(upcomingStatuses, doctorUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ConsultationResponse updatePriority(Long consultationId, String newPriority) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found: " + consultationId));

        Consultation.Priority priority = Consultation.Priority.valueOf(newPriority.toUpperCase());
        consultation.setPriority(priority);

        Consultation saved = consultationRepository.save(consultation);
        log.info("Consultation id={} priority updated to {}", saved.getId(), saved.getPriority());

        return toResponse(saved);
    }

    private String serializeChatContext(List<ConversationMessage> messages) {
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize chat context, falling back to plain text", e);
            return messages.stream()
                    .map(m -> m.getRole() + ": " + m.getContent())
                    .collect(Collectors.joining("\n"));
        }
    }

    private Consultation.Priority resolvePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return Consultation.Priority.NORMAL;
        }
        try {
            return Consultation.Priority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Consultation.Priority.NORMAL;
        }
    }

    private Consultation.ConsultationStatus parseStatus(String raw) {
        try {
            return Consultation.ConsultationStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid consultation status: " + raw);
        }
    }

    private void requireOwner(Long ownerId, Long doctorUserId, Long consultationId) {
        if (ownerId == null || !ownerId.equals(doctorUserId)) {
            throw new ConsultationConflictException(
                    "Only the doctor assigned to consultation " + consultationId
                            + " can perform this action.");
        }
    }

    private ConsultationResponse toResponse(Consultation consultation, List<ConversationMessage> messages) {
        ConsultationResponse response = toResponse(consultation);
        response.setChatContextSummary(messages.size() > 0
                ? messages.size() + " messages in conversation"
                : "No messages");
        response.setMessageCount(messages.size());
        return response;
    }

    private ConsultationResponse toResponse(Consultation consultation) {
        String patientName = userRepository.findById(consultation.getPatientUserId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Unknown Patient");

        String doctorName = null;
        if (consultation.getDoctorUserId() != null) {
            doctorName = userRepository.findById(consultation.getDoctorUserId())
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .orElse(null);
        }

        return ConsultationResponse.builder()
                .id(consultation.getId())
                .conversationId(consultation.getConversationId())
                .patientUserId(consultation.getPatientUserId())
                .patientName(patientName)
                .doctorUserId(consultation.getDoctorUserId())
                .doctorName(doctorName)
                .status(consultation.getStatus().name())
                .priority(consultation.getPriority().name())
                .reason(consultation.getReason())
                .doctorNotes(consultation.getDoctorNotes())
                .rejectionReason(consultation.getRejectionReason())
                .scheduledAt(consultation.getScheduledAt())
                .messageCount(0)
                .chatContextSummary("")
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt())
                .build();
    }
}
