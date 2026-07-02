package org.example.Healthcareplatform.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound chat request from the frontend.
 *
 * <pre>{@code
 * {
 *   "conversationId": 1,        // optional — creates new conversation if null
 *   "message": "I have a headache.",
 *   "userId": 1                 // optional — defaults to 1 for MVP
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /** Optional — if null, the service creates a new conversation. */
    private Long conversationId;

    /** The user's chat message (required). */
    private String message;

    /** Optional — the user sending the message (defaults to 1 for MVP). */
    private Long userId;
}
