package org.example.Healthcareplatform.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * A single message within a conversation — either user input or AI response.
 */
@Entity
@Table(name = "conversation_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The conversation this message belongs to. */
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    /** Who sent this message: USER or ASSISTANT. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    /** The message text content. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** AI provider name (e.g. "OpenRouter"), null for user messages. */
    private String provider;

    /** Model identifier (e.g. "google/gemini-2.0-flash-001"), null for user messages. */
    private String model;

    /** Token count from the provider response, null when not available. */
    private Integer tokenUsage;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    public enum MessageRole {
        USER,
        ASSISTANT,
        SYSTEM
    }
}
