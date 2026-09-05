package org.example.Healthcareplatform.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * The user's health profile: allergies, chronic conditions,
 * emergency contacts, consent and privacy settings.
 */
@Data
@Builder
public class HealthProfileResponse {

    private List<String> allergies;
    private List<String> chronicConditions;
    private List<EmergencyContactDTO> emergencyContacts;

    private boolean dataProcessingConsent;
    private boolean emailNotifications;
    private boolean profileVisible;
    private boolean shareHealthDataWithDoctors;
}
