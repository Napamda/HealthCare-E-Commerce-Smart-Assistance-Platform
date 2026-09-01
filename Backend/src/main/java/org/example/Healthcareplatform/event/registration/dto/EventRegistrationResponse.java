package org.example.Healthcareplatform.event.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Healthcareplatform.event.registration.entity.RegistrationStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistrationResponse {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private String category;
    private LocalDateTime startDateTime;
    private String venue;
    private String city;
    private String organizer;
    private RegistrationStatus status;
    private LocalDateTime registeredAt;
}
