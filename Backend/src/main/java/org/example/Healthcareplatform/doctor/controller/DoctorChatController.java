package org.example.Healthcareplatform.doctor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.dto.ChatResponse;
import org.example.Healthcareplatform.ai.dto.ConversationResponse;
import org.example.Healthcareplatform.ai.entity.Conversation;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.example.Healthcareplatform.ai.service.ConversationService;
import org.example.Healthcareplatform.ai.service.DoctorChatService;
import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints that let a doctor view the chats of consultations they have accepted
 * and reply to the patient. Replies are persisted with the DOCTOR message role
 * so they are clearly labeled as coming from the doctor.
 */
@Slf4j
@RestController
@RequestMapping("/api/doctor/conversations")
@RequiredArgsConstructor
public class DoctorChatController {

    private final DoctorChatService doctorChatService;
    private final ConversationService conversationService;
    private final SecurityContextUtil securityContextUtil;

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> listDoctorConversations() {
        Long doctorUserId = securityContextUtil.getCurrentUserId();
        log.info("GET /api/doctor/conversations — doctorUserId={}", doctorUserId);
        return ResponseEntity.ok(doctorChatService.listDoctorConversations(doctorUserId));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<?> getConversation(@PathVariable Long conversationId) {
        Long doctorUserId = securityContextUtil.getCurrentUserId();
        log.info("GET /api/doctor/conversations/{} — doctorUserId={}", conversationId, doctorUserId);

        if (!doctorChatService.isAssignedToConversation(conversationId, doctorUserId)) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Doctor is not assigned to an active consultation for this conversation"));
        }

        Conversation conversation = conversationService.findConversation(conversationId);
        List<ConversationMessage> messages = conversationService.getMessages(conversationId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", conversation.getId());
        body.put("title", conversation.getTitle());
        body.put("status", conversation.getStatus().name());
        body.put("createdAt", conversation.getCreatedAt());
        body.put("updatedAt", conversation.getUpdatedAt());
        body.put("messages", messages);
        body.put("session", doctorChatService.activeSessionInfo(conversationId));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<?> sendDoctorMessage(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> payload) {
        Long doctorUserId = securityContextUtil.getCurrentUserId();
        String content = payload == null ? null : payload.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "content must not be blank"));
        }

        log.info("POST /api/doctor/conversations/{}/messages — doctorUserId={}", conversationId, doctorUserId);

        try {
            ChatResponse response = doctorChatService
                    .sendDoctorMessage(conversationId, content, doctorUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{conversationId}/session")
    public ResponseEntity<?> getSession(@PathVariable Long conversationId) {
        Long doctorUserId = securityContextUtil.getCurrentUserId();
        log.info("GET /api/doctor/conversations/{}/session — doctorUserId={}", conversationId, doctorUserId);

        if (!doctorChatService.isAssignedToConversation(conversationId, doctorUserId)) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Doctor is not assigned to an active consultation for this conversation"));
        }

        return ResponseEntity.ok(doctorChatService.activeSessionInfo(conversationId));
    }
}
