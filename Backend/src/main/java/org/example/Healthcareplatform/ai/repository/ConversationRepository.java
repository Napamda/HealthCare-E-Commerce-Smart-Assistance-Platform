package org.example.Healthcareplatform.ai.repository;

import org.example.Healthcareplatform.ai.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, Conversation.ConversationStatus status);

    List<Conversation> findByUserIdAndStatusNotOrderByUpdatedAtDesc(Long userId, Conversation.ConversationStatus status);

    Page<Conversation> findByUserIdAndStatusNot(Long userId, Conversation.ConversationStatus status, Pageable pageable);
}
