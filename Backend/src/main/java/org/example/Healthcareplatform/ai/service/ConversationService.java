package org.example.Healthcareplatform.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.entity.Conversation;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.example.Healthcareplatform.ai.repository.ConversationMessageRepository;
import org.example.Healthcareplatform.ai.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages conversation lifecycle and message persistence.
 * <p>
 * <b>Responsibility boundary:</b> This service handles only persistence.
 * It does NOT contain AI logic, prompt building, or provider calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;

    // ---------------------------------------------------------------
    // Conversation management
    // ---------------------------------------------------------------

    /**
     * Create a new conversation for the given user.
     * The title is derived from the first user message.
     */
    @Transactional
    public Conversation createConversation(Long userId, String firstMessage) {
        // Derive a short title from the first message
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

    /**
     * Find a conversation by ID, or throw if not found.
     */
    public Conversation findConversation(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + id));
    }

    /**
     * Find or create a conversation: if conversationId is provided, load it;
     * otherwise, create a new one.
     *
     * @param conversationId optional existing conversation ID (may be null)
     * @param userId         the owning user
     * @param firstMessage   used for title if creating a new conversation
     * @return the existing or newly created conversation
     */
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

    /**
     * List all non-deleted conversations for a user, newest first.
     */
    public List<Conversation> listConversations(Long userId) {
        return conversationRepository
                .findByUserIdAndStatusNotOrderByUpdatedAtDesc(
                        userId, Conversation.ConversationStatus.DELETED);
    }

    /**
     * Soft-delete a conversation by marking it as DELETED.
     */
    @Transactional
    public void deleteConversation(Long id) {
        Conversation conversation = findConversation(id);
        conversation.setStatus(Conversation.ConversationStatus.DELETED);
        conversationRepository.save(conversation);
        log.info("Soft-deleted conversation id={}", id);
    }

    // ---------------------------------------------------------------
    // Message management
    // ---------------------------------------------------------------

    /**
     * Persist a user message.
     */
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

    /**
     * Persist an AI assistant response.
     */
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

    /**
     * Retrieve all messages for a conversation, oldest first.
     */
    public List<ConversationMessage> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * Derive a short title from the user's first message.
     */
    private String deriveTitle(String message) {
        if (message == null || message.isBlank()) {
            return "New conversation";
        }
        // Truncate to 80 chars and strip trailing punctuation
        String trimmed = message.strip();
        if (trimmed.length() > 80) {
            trimmed = trimmed.substring(0, 80) + "...";
        }
        return trimmed;
    }
}
