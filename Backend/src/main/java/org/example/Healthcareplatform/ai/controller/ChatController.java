package org.example.Healthcareplatform.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.dto.ChatRequest;
import org.example.Healthcareplatform.ai.dto.ChatResponse;
import org.example.Healthcareplatform.ai.dto.ConversationResponse;
import org.example.Healthcareplatform.ai.entity.Conversation;
import org.example.Healthcareplatform.ai.exception.AIException;
import org.example.Healthcareplatform.ai.service.AIService;
import org.example.Healthcareplatform.ai.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the AI Chat feature.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST   /api/chat}                       — send a message to the AI</li>
 *   <li>{@code POST   /api/chat/test}                  — health-check / connectivity test</li>
 *   <li>{@code GET    /api/chat/conversations}          — list user conversations</li>
 *   <li>{@code GET    /api/chat/conversations/{id}}     — get conversation details</li>
 *   <li>{@code DELETE /api/chat/conversations/{id}}     — soft-delete a conversation</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AIService aiService;
    private final ConversationService conversationService;

    // ================================================================
    // Chat
    // ================================================================

    /**
     * Main chat endpoint.
     *
     * <pre>{@code
     * POST /api/chat
     * Content-Type: application/json
     *
     * {
     *   "conversationId": null,
     *   "message": "I have a headache.",
     *   "userId": 1
     * }
     * }</pre>
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("POST /api/chat — message length: {}",
                request.getMessage() != null ? request.getMessage().length() : 0);

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ChatResponse response = aiService.chat(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint — verifies the controller/service/provider chain
     * without requiring a real API key (works with Mock too).
     */
    @PostMapping("/test")
    public ResponseEntity<ChatResponse> test(@RequestBody(required = false) ChatRequest request) {
        String message = (request != null && request.getMessage() != null)
                ? request.getMessage()
                : "Hello, this is a connectivity test.";

        Long userId = (request != null) ? request.getUserId() : null;
        Long conversationId = (request != null) ? request.getConversationId() : null;

        log.info("POST /api/chat/test — message: {}", message);

        ChatRequest testRequest = ChatRequest.builder()
                .conversationId(conversationId)
                .message(message)
                .userId(userId)
                .build();

        ChatResponse response = aiService.chat(testRequest);
        return ResponseEntity.ok(response);
    }

    // ================================================================
    // Conversation management
    // ================================================================

    /**
     * List all conversations for a user.
     *
     * <pre>{@code
     * GET /api/chat/conversations?userId=1
     * }</pre>
     *
     * @param userId optional query param (defaults to 1 for MVP)
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> listConversations(
            @RequestParam(defaultValue = "1") Long userId) {
        log.info("GET /api/chat/conversations — userId={}", userId);

        List<Conversation> conversations = conversationService.listConversations(userId);

        List<ConversationResponse> responses = conversations.stream()
                .map(this::toConversationResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Get a single conversation with its messages.
     *
     * <pre>{@code
     * GET /api/chat/conversations/1
     * }</pre>
     */
    @GetMapping("/conversations/{id}")
    public ResponseEntity<?> getConversation(@PathVariable Long id) {
        log.info("GET /api/chat/conversations/{}", id);

        try {
            Conversation conversation = conversationService.findConversation(id);
            var messages = conversationService.getMessages(id);

            Map<String, Object> response = Map.of(
                    "id", conversation.getId(),
                    "title", conversation.getTitle(),
                    "status", conversation.getStatus().name(),
                    "createdAt", conversation.getCreatedAt(),
                    "updatedAt", conversation.getUpdatedAt(),
                    "messages", messages
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Soft-delete a conversation.
     *
     * <pre>{@code
     * DELETE /api/chat/conversations/1
     * }</pre>
     */
    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        log.info("DELETE /api/chat/conversations/{}", id);

        try {
            conversationService.deleteConversation(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ================================================================
    // Exception handlers
    // ================================================================

    @org.springframework.web.bind.annotation.ExceptionHandler(AIException.class)
    public ResponseEntity<Map<String, String>> handleAIException(AIException ex) {
        log.error("AI error: {}", ex.getMessage());
        return ResponseEntity.internalServerError()
                .body(Map.of("error", ex.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
    }

    // ================================================================
    // Helpers
    // ================================================================

    private ConversationResponse toConversationResponse(Conversation conversation) {
        long messageCount = conversationService.getMessages(conversation.getId()).size();
        return ConversationResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .status(conversation.getStatus().name())
                .messageCount(messageCount)
                .build();
    }
}
