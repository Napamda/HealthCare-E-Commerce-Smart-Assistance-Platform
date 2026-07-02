package org.example.Healthcareplatform.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response returned after each AI chat turn.
 *
 * <pre>{@code
 * {
 *   "conversationId": 1,
 *   "messageId": 42,
 *   "response": "Based on your symptoms...",
 *   "provider": "OpenRouter",
 *   "model": "google/gemini-2.0-flash-001",
 *   "timestamp": "2026-07-02T11:00:00Z"
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** The conversation this message belongs to (useful for continuing the chat). */
    private Long conversationId;

    /** The persisted ID of the assistant message (for analytics / debugging). */
    private Long messageId;

    /** The AI-generated reply text. */
    private String response;

    /** Which provider generated this, e.g. "OpenRouter", "Mock". */
    private String provider;

    /** The model identifier used, e.g. "google/gemini-2.0-flash-001". */
    private String model;

    /** When the response was generated. */
    @Builder.Default
    private Instant timestamp = Instant.now();
}
