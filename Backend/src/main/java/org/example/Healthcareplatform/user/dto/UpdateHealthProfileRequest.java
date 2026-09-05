package org.example.Healthcareplatform.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Save-request for the health profile. Lists are replaced wholesale
 * (the frontend sends the full updated lists).
 */
@Data
public class UpdateHealthProfileRequest {

    @Size(max = 30, message = "At most 30 allergies")
    private List<@Size(max = 200, message = "Allergy must be at most 200 characters") String> allergies;

    @Size(max = 30, message = "At most 30 chronic conditions")
    private List<@Size(max = 200, message = "Condition must be at most 200 characters") String> chronicConditions;

    @Size(max = 10, message = "At most 10 emergency contacts")
    private List<EmergencyContactDTO> emergencyContacts;

    private Boolean dataProcessingConsent;

    private Boolean emailNotifications;

    private Boolean profileVisible;

    private Boolean shareHealthDataWithDoctors;
}
