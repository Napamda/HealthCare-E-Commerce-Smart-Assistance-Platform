package org.example.Healthcareplatform.ai.repository;

import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access for {@link ConversationMessage} entities.
 * Exposes only persistence methods — no business logic.
 */
@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    /** Retrieve all messages for a conversation, ordered oldest-first. */
    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /** Count messages in a conversation. */
    long countByConversationId(Long conversationId);

    /** Delete all messages belonging to a conversation. */
    void deleteByConversationId(Long conversationId);
}
