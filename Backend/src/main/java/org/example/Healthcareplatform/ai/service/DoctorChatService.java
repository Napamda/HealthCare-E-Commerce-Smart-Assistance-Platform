package org.example.Healthcareplatform.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.dto.ChatResponse;
import org.example.Healthcareplatform.ai.dto.ConversationResponse;
import org.example.Healthcareplatform.ai.entity.Conversation;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.example.Healthcareplatform.consultation.entity.Consultation;
import org.example.Healthcareplatform.consultation.repository.ConsultationRepository;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorChatService {

    private static final List<Consultation.ConsultationStatus> ACTIVE_SESSION_STATUSES =
            List.of(Consultation.ConsultationStatus.ACCEPTED, Consultation.ConsultationStatus.IN_PROGRESS);

    private final ConsultationRepository consultationRepository;
    private final ConversationService conversationService;
    private final UserRepository userRepository;

    public boolean isAssignedToConversation(Long conversationId, Long doctorUserId) {
        return consultationRepository
                .findFirstByConversationIdAndStatusIn(conversationId, ACTIVE_SESSION_STATUSES)
                .map(consultation -> consultation.getDoctorUserId() != null
                        && consultation.getDoctorUserId().equals(doctorUserId))
                .orElse(false);
    }

    public List<ConversationResponse> listDoctorConversations(Long doctorUserId) {
        return consultationRepository
                .findByDoctorUserIdAndStatusInOrderByUpdatedAtDesc(doctorUserId, ACTIVE_SESSION_STATUSES)
                .stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional
    public ChatResponse sendDoctorMessage(Long conversationId, String content, Long doctorUserId) {
        Consultation consultation = consultationRepository
                .findFirstByConversationIdAndStatusIn(conversationId, ACTIVE_SESSION_STATUSES)
                .filter(c -> c.getDoctorUserId() != null && c.getDoctorUserId().equals(doctorUserId))
                .orElseThrow(() -> new IllegalStateException(
                        "Doctor is not assigned to an active session for this conversation"));

        ConversationMessage saved = conversationService.saveDoctorMessage(conversationId, content);

        log.info("Doctor userId={} sent message id={} in consultation id={} (conversation {})",
                doctorUserId, saved.getId(), consultation.getId(), conversationId);

        return ChatResponse.builder()
                .conversationId(conversationId)
                .messageId(saved.getId())
                .response(content)
                .mode("DOCTOR")
                .timestamp(Instant.now())
                .build();
    }

    public Map<String, Object> activeSessionInfo(Long conversationId) {
        return consultationRepository
                .findFirstByConversationIdAndStatusIn(conversationId, ACTIVE_SESSION_STATUSES)
                .map(consultation -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("consultationId", consultation.getId());
                    info.put("status", consultation.getStatus().name());
                    info.put("priority", consultation.getPriority().name());
                    userRepository.findById(consultation.getDoctorUserId())
                            .ifPresent(user -> info.put("doctorName",
                                    user.getFirstName() + " " + user.getLastName()));
                    return info;
                })
                .orElse(null);
    }

    private ConversationResponse toConversationResponse(Consultation consultation) {
        Long conversationId = consultation.getConversationId();
        Conversation conversation = conversationService.findConversation(conversationId);
        String patientName = userRepository.findById(consultation.getPatientUserId())
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse("Patient");

        return ConversationResponse.builder()
                .id(conversationId)
                .title(patientName)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .status(conversation.getStatus().name())
                .messageCount(conversationService.getMessages(conversationId).size())
                .consultationId(consultation.getId())
                .patientName(patientName)
                .consultationStatus(consultation.getStatus().name())
                .priority(consultation.getPriority().name())
                .build();
    }
}
