package org.example.Healthcareplatform.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

/**
 * One emergency contact entry.
 */
@Data
@Builder
public class EmergencyContactDTO {

    @Size(max = 100, message = "Contact name must be at most 100 characters")
    private String name;

    @Pattern(regexp = "^\\+?[0-9 ()\\-]{7,20}$", message = "Contact phone must be 7-20 digits, optionally starting with +")
    private String phone;

    @Size(max = 50, message = "Relationship must be at most 50 characters")
    private String relationship;
}
