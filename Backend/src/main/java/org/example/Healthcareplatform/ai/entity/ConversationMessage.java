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

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String provider;

    private String model;

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
