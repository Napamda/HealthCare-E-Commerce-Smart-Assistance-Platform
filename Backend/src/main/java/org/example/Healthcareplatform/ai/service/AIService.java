package org.example.Healthcareplatform.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.dto.ChatRequest;
import org.example.Healthcareplatform.ai.dto.ChatResponse;
import org.example.Healthcareplatform.ai.entity.Conversation;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.example.Healthcareplatform.ai.provider.AIProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Core AI chat orchestrator.
 *
 * <h3>Flow (Phase 5)</h3>
 * <pre>{@code
 *   ChatRequest
 *        │
 *        ▼
 *   findOrCreateConversation   ←── ConversationService
 *        │
 *        ▼
 *   saveUserMessage            ←── ConversationService
 *        │
 *        ▼
 *   loadAllMessages            ←── ConversationService
 *        │
 *        ├── totalMsgs > threshold?  →  ConversationSummaryService.summarize(oldMsgs)
 *        │
 *        ▼
 *   trim to last N             ←── configurable ai.memory.max-history
 *        │
 *        ▼
 *   buildPrompt(msg, history, summary)  ←── PromptService
 *        │
 *        ▼
 *   AIProvider.chat(prompt)    ←── OpenRouter / Mock
 *        │
 *        ▼
 *   saveAssistantMessage       ←── ConversationService
 *        │
 *        ▼
 *   ChatResponse
 * }</pre>
 * <p>
 * AIService is the orchestrator — it delegates persistence to
 * {@link ConversationService}, summarization to {@link ConversationSummaryService},
 * and prompt building to {@link PromptService}. It contains no persistence,
 * prompt, or summary logic itself.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final AIProvider aiProvider;
    private final PromptService promptService;
    private final ConversationService conversationService;
    private final ConversationSummaryService summaryService;

    /** Default user ID when the request does not provide one. */
    private static final Long DEFAULT_USER_ID = 1L;

    /**
     * Maximum number of recent messages to include in the prompt context.
     * Configurable via {@code ai.memory.max-history} in application.yml.
     */
    @Value("${ai.memory.max-history:10}")
    private int maxHistory;

    /**
     * When total messages in a conversation exceed this threshold,
     * older messages are replaced with a compact summary.
     * Configurable via {@code ai.memory.summary-threshold} in application.yml.
     */
    @Value("${ai.memory.summary-threshold:30}")
    private int summaryThreshold;

    /**
     * Process a chat message end-to-end with full persistence and
     * configurable context memory.
     */
    @Transactional
    public ChatResponse chat(ChatRequest request) {
        Long userId = resolveUserId(request);
        log.info("AIService.chat — provider={}, model={}, userId={}, convoId={}, maxHistory={}, summaryThreshold={}",
                aiProvider.providerName(), aiProvider.modelName(),
                userId, request.getConversationId(), maxHistory, summaryThreshold);

        // --------------------------------------------------
        // 1. Find or create the conversation
        // --------------------------------------------------
        Conversation conversation = conversationService.findOrCreateConversation(
                request.getConversationId(), userId, request.getMessage());

        // --------------------------------------------------
        // 2. Persist the user message
        // --------------------------------------------------
        conversationService.saveUserMessage(conversation.getId(), request.getMessage());

        // --------------------------------------------------
        // 3. Load all messages (we'll decide what to inject)
        // --------------------------------------------------
        List<ConversationMessage> allMessages = conversationService
                .getMessages(conversation.getId());

        // --------------------------------------------------
        // 4. Build context: summary (old messages) + last N recent
        // --------------------------------------------------
        String summary = "";
        List<ConversationMessage> contextHistory;

        int totalMessages = allMessages.size();

        if (totalMessages > summaryThreshold) {
            // Conversation is large — summarize older messages,
            // only keep the last N in raw form
            int splitPoint = totalMessages - maxHistory;
            List<ConversationMessage> olderMessages = allMessages.subList(0, splitPoint);
            contextHistory = allMessages.subList(splitPoint, totalMessages);

            summary = summaryService.summarize(olderMessages);
            log.info("Conversation id={} has {} messages — summarized {} older, keeping {} recent",
                    conversation.getId(), totalMessages, olderMessages.size(), contextHistory.size());
        } else if (totalMessages > maxHistory) {
            // Enough messages to trim but not enough to summarize
            contextHistory = allMessages.subList(
                    totalMessages - maxHistory, totalMessages);
        } else {
            // Small conversation — use all messages
            contextHistory = allMessages;
        }

        // --------------------------------------------------
        // 5. Build the full prompt (system + summary + history + current)
        // --------------------------------------------------
        String prompt = promptService.buildPrompt(
                request.getMessage(), contextHistory, summary);

        // --------------------------------------------------
        // 6. Call the AI provider (pure String → pure String)
        // --------------------------------------------------
        String responseText = aiProvider.chat(prompt);

        // --------------------------------------------------
        // 7. Persist the AI response
        // --------------------------------------------------
        ConversationMessage saved = conversationService.saveAssistantMessage(
                conversation.getId(), responseText,
                aiProvider.providerName(), aiProvider.modelName());

        // --------------------------------------------------
        // 8. Build the enriched response DTO
        // --------------------------------------------------
        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .messageId(saved.getId())
                .response(responseText)
                .provider(aiProvider.providerName())
                .model(aiProvider.modelName())
                .timestamp(Instant.now())
                .build();
    }

    // ---- helpers ----

    private Long resolveUserId(ChatRequest request) {
        return (request.getUserId() != null) ? request.getUserId() : DEFAULT_USER_ID;
    }
}
