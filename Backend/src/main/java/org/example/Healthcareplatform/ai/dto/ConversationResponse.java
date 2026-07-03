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
public class ConversationResponse {

    private Long id;
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
    private String status;
    private long messageCount;
}
