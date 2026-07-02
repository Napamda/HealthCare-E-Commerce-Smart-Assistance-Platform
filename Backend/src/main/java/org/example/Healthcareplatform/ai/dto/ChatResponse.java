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

    @Builder.Default
    private Instant timestamp = Instant.now();
}
