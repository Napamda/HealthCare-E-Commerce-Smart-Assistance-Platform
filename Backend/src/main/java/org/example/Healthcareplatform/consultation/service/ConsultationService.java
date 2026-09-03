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
import org.example.Healthcareplatform.consultation.repository.ConsultationRepository;
import org.example.Healthcareplatform.notification.entity.Notification;
import org.example.Healthcareplatform.notification.service.NotificationService;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found: " + consultationId));

        Consultation.ConsultationStatus status = Consultation.ConsultationStatus.valueOf(newStatus.toUpperCase());

        consultation.setStatus(status);
        if (doctorUserId != null) {
            consultation.setDoctorUserId(doctorUserId);
        }
        if (rejectionReason != null && !rejectionReason.isBlank()) {
            consultation.setRejectionReason(rejectionReason);
        }
        if (scheduledAt != null) {
            consultation.setScheduledAt(scheduledAt);
        }

        Consultation saved = consultationRepository.save(consultation);
        log.info("Consultation id={} status updated to {} by doctorUserId={}",
                saved.getId(), saved.getStatus(), doctorUserId);

        // Notify the patient when a doctor accepts or starts the consultation.
        if (status == Consultation.ConsultationStatus.ACCEPTED
                || status == Consultation.ConsultationStatus.IN_PROGRESS) {
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
        }

        return toResponse(saved);
    }

    public List<ConsultationResponse> getDoctorQueue(Long doctorUserId) {
        List<Consultation.ConsultationStatus> activeStatuses = List.of(
                Consultation.ConsultationStatus.PENDING,
                Consultation.ConsultationStatus.ACCEPTED,
                Consultation.ConsultationStatus.IN_PROGRESS);
        return consultationRepository.findByStatusInOrderByPriorityAscCreatedAtAsc(activeStatuses)
                .stream()
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
