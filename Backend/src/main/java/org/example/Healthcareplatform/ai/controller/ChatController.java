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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AIService aiService;
    private final ConversationService conversationService;

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

    @GetMapping("/conversations")
    public ResponseEntity<?> listConversations(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        log.info("GET /api/chat/conversations — userId={}, page={}, size={}", userId, page, size);

        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
            Page<Conversation> conversationPage = conversationService.listConversations(userId, pageable);

            List<ConversationResponse> items = conversationPage.getContent().stream()
                    .map(this::toConversationResponse)
                    .toList();

            Map<String, Object> body = Map.of(
                    "content", items,
                    "page", conversationPage.getNumber(),
                    "size", conversationPage.getSize(),
                    "totalElements", conversationPage.getTotalElements(),
                    "totalPages", conversationPage.getTotalPages(),
                    "first", conversationPage.isFirst(),
                    "last", conversationPage.isLast()
            );
            return ResponseEntity.ok(body);
        }

        List<Conversation> conversations = conversationService.listConversations(userId);
        List<ConversationResponse> responses = conversations.stream()
                .map(this::toConversationResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

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

    @org.springframework.web.bind.annotation.ExceptionHandler(AIException.class)
    public ResponseEntity<Map<String, String>> handleAIException(AIException ex) {
        log.error("AI error: {}", ex.getMessage(), ex);
        String detail = (ex.getCause() != null) ? ex.getCause().getMessage() : ex.getMessage();
        return ResponseEntity.internalServerError()
                .body(Map.of(
                        "error", ex.getMessage(),
                        "detail", detail
                ));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
    }

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
