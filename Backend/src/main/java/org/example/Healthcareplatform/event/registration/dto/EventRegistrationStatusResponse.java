package org.example.Healthcareplatform.event.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistrationStatusResponse {

    private long count;
    private Integer capacity;
    private boolean registered;
    private Long registrationId;

    /**
     * The volunteer role stored on the existing registration (DOCTOR or
     * PHARMACIST) when the registrant is a volunteer. Null for regular
     * attendees or when not registered.
     */
    private String volunteerRole;
}
