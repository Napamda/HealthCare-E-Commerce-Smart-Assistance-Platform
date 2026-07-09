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
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.repository.UserRepository;
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

        publishNotification(saved);

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
    public ConsultationResponse updateStatus(Long consultationId, String newStatus, Long doctorUserId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found: " + consultationId));

        Consultation.ConsultationStatus status = Consultation.ConsultationStatus.valueOf(newStatus.toUpperCase());

        consultation.setStatus(status);
        if (doctorUserId != null) {
            consultation.setDoctorUserId(doctorUserId);
        }

        Consultation saved = consultationRepository.save(consultation);
        log.info("Consultation id={} status updated to {} by doctorUserId={}",
                saved.getId(), saved.getStatus(), doctorUserId);

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

    private void publishNotification(Consultation consultation) {
        ConsultationCreatedEvent event = ConsultationCreatedEvent.builder()
                .consultationId(consultation.getId())
                .conversationId(consultation.getConversationId())
                .patientUserId(consultation.getPatientUserId())
                .reason(consultation.getReason())
                .priority(consultation.getPriority().name())
                .createdAt(Instant.now())
                .build();

        log.info("Notification event published — consultationId={}, patientUserId={}, priority={}",
                event.getConsultationId(), event.getPatientUserId(), event.getPriority());
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
                .messageCount(0)
                .chatContextSummary("")
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt())
                .build();
    }
}
