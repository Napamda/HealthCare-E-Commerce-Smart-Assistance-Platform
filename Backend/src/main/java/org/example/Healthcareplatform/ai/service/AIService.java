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

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final AIProvider aiProvider;
    private final PromptService promptService;
    private final ConversationService conversationService;
    private final ConversationSummaryService summaryService;

    private static final Long DEFAULT_USER_ID = 1L;

    @Value("${ai.memory.max-history:10}")
    private int maxHistory;

    @Value("${ai.memory.summary-threshold:30}")
    private int summaryThreshold;

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        Long userId = resolveUserId(request);
        log.info("AIService.chat — provider={}, model={}, userId={}, convoId={}, maxHistory={}, summaryThreshold={}",
                aiProvider.providerName(), aiProvider.modelName(),
                userId, request.getConversationId(), maxHistory, summaryThreshold);

        Conversation conversation = conversationService.findOrCreateConversation(
                request.getConversationId(), userId, request.getMessage());

        conversationService.saveUserMessage(conversation.getId(), request.getMessage());

        List<ConversationMessage> allMessages = conversationService
                .getMessages(conversation.getId());

        String summary = "";
        List<ConversationMessage> contextHistory;

        int totalMessages = allMessages.size();

        if (totalMessages > summaryThreshold) {
            int splitPoint = totalMessages - maxHistory;
            List<ConversationMessage> olderMessages = allMessages.subList(0, splitPoint);
            contextHistory = allMessages.subList(splitPoint, totalMessages);

            summary = summaryService.summarize(olderMessages);
            log.info("Conversation id={} has {} messages — summarized {} older, keeping {} recent",
                    conversation.getId(), totalMessages, olderMessages.size(), contextHistory.size());
        } else if (totalMessages > maxHistory) {
            contextHistory = allMessages.subList(
                    totalMessages - maxHistory, totalMessages);
        } else {
            contextHistory = allMessages;
        }

        String prompt = promptService.buildPrompt(
                request.getMessage(), contextHistory, summary);

        String responseText = aiProvider.chat(prompt);

        ConversationMessage saved = conversationService.saveAssistantMessage(
                conversation.getId(), responseText,
                aiProvider.providerName(), aiProvider.modelName());

        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .messageId(saved.getId())
                .response(responseText)
                .provider(aiProvider.providerName())
                .model(aiProvider.modelName())
                .timestamp(Instant.now())
                .build();
    }

    private Long resolveUserId(ChatRequest request) {
        return (request.getUserId() != null) ? request.getUserId() : DEFAULT_USER_ID;
    }
}
