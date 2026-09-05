package org.example.Healthcareplatform.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationEventResponse {
    private Long id;
    private String title;
    private String category;
    private String organizer;
    private String city;
    private String status;
    private String moderationReason;
    private Long moderatedBy;
    private LocalDateTime startDateTime;
    private LocalDateTime createdAt;
}
