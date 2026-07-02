package org.example.Healthcareplatform.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.entity.Conversation;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.example.Healthcareplatform.ai.repository.ConversationMessageRepository;
import org.example.Healthcareplatform.ai.repository.ConversationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;

    @Transactional
    public Conversation createConversation(Long userId, String firstMessage) {
        String title = deriveTitle(firstMessage);

        Conversation conversation = Conversation.builder()
                .userId(userId)
                .title(title)
                .status(Conversation.ConversationStatus.ACTIVE)
                .build();

        Conversation saved = conversationRepository.save(conversation);
        log.info("Created conversation id={} for userId={}", saved.getId(), userId);
        return saved;
    }

    public Conversation findConversation(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + id));
    }

    @Transactional
    public Conversation findOrCreateConversation(Long conversationId, Long userId, String firstMessage) {
        if (conversationId != null) {
            Conversation existing = findConversation(conversationId);
            if (!existing.getUserId().equals(userId)) {
                throw new IllegalArgumentException("Conversation does not belong to user");
            }
            if (existing.getStatus() == Conversation.ConversationStatus.DELETED) {
                throw new IllegalArgumentException("Conversation has been deleted");
            }
            return existing;
        }
        return createConversation(userId, firstMessage);
    }

    public List<Conversation> listConversations(Long userId) {
        return conversationRepository
                .findByUserIdAndStatusNotOrderByUpdatedAtDesc(
                        userId, Conversation.ConversationStatus.DELETED);
    }

    public Page<Conversation> listConversations(Long userId, Pageable pageable) {
        return conversationRepository
                .findByUserIdAndStatusNot(userId, Conversation.ConversationStatus.DELETED, pageable);
    }

    @Transactional
    public void deleteConversation(Long id) {
        Conversation conversation = findConversation(id);
        conversation.setStatus(Conversation.ConversationStatus.DELETED);
        conversationRepository.save(conversation);
        log.info("Soft-deleted conversation id={}", id);
    }


    @Transactional
    public ConversationMessage saveUserMessage(Long conversationId, String content) {
        ConversationMessage message = ConversationMessage.builder()
                .conversationId(conversationId)
                .role(ConversationMessage.MessageRole.USER)
                .content(content)
                .build();

        ConversationMessage saved = messageRepository.save(message);
        log.debug("Saved user message id={} for conversationId={}", saved.getId(), conversationId);
        return saved;
    }

    @Transactional
    public ConversationMessage saveAssistantMessage(Long conversationId, String content,
                                                     String provider, String model) {
        ConversationMessage message = ConversationMessage.builder()
                .conversationId(conversationId)
                .role(ConversationMessage.MessageRole.ASSISTANT)
                .content(content)
                .provider(provider)
                .model(model)
                .build();

        ConversationMessage saved = messageRepository.save(message);
        log.debug("Saved assistant message id={} for conversationId={}", saved.getId(), conversationId);
        return saved;
    }

    public List<ConversationMessage> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    private String deriveTitle(String message) {
        if (message == null || message.isBlank()) {
            return "New conversation";
        }
        String trimmed = message.strip();
        if (trimmed.length() > 80) {
            trimmed = trimmed.substring(0, 80) + "...";
        }
        return trimmed;
    }
}
