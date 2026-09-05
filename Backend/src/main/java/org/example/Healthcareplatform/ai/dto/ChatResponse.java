package org.example.Healthcareplatform.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private Long conversationId;

    private Long messageId;

    private String response;

    private String provider;

    private String model;

    /**
     * Indicates the source of the message — "AI" for assistant replies and
     * "DOCTOR" for messages sent by a doctor through the doctor chat endpoints.
     * Null implies a normal AI response for backward compatibility.
     */
    @Builder.Default
    private String mode = "AI";

    @Builder.Default
    private Instant timestamp = Instant.now();
}
