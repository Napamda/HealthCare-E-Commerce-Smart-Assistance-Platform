package org.example.Healthcareplatform.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Lightweight DTO for listing conversations.
 *
 * <pre>{@code
 * {
 *   "id": 1,
 *   "title": "I have a headache...",
 *   "createdAt": "2026-07-02T11:00:00Z",
 *   "updatedAt": "2026-07-02T11:30:00Z",
 *   "status": "ACTIVE",
 *   "messageCount": 6
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private Long id;
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
    private String status;
    private long messageCount;
}
