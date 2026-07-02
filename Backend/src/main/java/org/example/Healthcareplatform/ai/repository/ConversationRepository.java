package org.example.Healthcareplatform.ai.repository;

import org.example.Healthcareplatform.ai.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access for {@link Conversation} entities.
 * Exposes only persistence methods — no business logic.
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /** Find all conversations for a user, ordered by most recent first. */
    List<Conversation> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, Conversation.ConversationStatus status);

    /** Find all non-deleted conversations for a user. */
    List<Conversation> findByUserIdAndStatusNotOrderByUpdatedAtDesc(Long userId, Conversation.ConversationStatus status);
}
