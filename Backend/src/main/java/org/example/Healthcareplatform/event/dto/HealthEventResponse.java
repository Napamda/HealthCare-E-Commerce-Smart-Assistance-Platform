package org.example.Healthcareplatform.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Healthcareplatform.event.entity.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthEventResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private List<String> tags;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String venue;
    private String city;
    private Double latitude;
    private Double longitude;
    private String organizer;
    private Integer capacity;
    private long registeredCount;
    private EventStatus status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
