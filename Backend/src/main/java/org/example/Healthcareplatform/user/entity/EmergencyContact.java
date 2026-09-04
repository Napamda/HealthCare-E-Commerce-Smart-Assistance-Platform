package org.example.Healthcareplatform.user.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An emergency contact stored inside a user's health profile
 * (element collection of an embeddable).
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContact {

    private String name;

    private String phone;

    private String relationship;
}
