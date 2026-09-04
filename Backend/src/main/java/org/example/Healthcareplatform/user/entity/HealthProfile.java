package org.example.Healthcareplatform.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-user health profile (Task 2.2): allergies, chronic conditions,
 * emergency contacts, consent management and privacy settings.
 * One row per user, created lazily on first save.
 */
@Entity
@Table(name = "health_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    // ---- Medical info ----
    @ElementCollection
    @CollectionTable(name = "health_profile_allergies",
            joinColumns = @JoinColumn(name = "health_profile_id"),
            foreignKey = @ForeignKey(name = "fk_health_allergies_profile"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Column(name = "value", length = 200)
    @Builder.Default
    private List<String> allergies = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "health_profile_conditions",
            joinColumns = @JoinColumn(name = "health_profile_id"),
            foreignKey = @ForeignKey(name = "fk_health_conditions_profile"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Column(name = "value", length = 200)
    @Builder.Default
    private List<String> chronicConditions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "health_profile_contacts",
            joinColumns = @JoinColumn(name = "health_profile_id"),
            foreignKey = @ForeignKey(name = "fk_health_contacts_profile"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Builder.Default
    private List<EmergencyContact> emergencyContacts = new ArrayList<>();

    // ---- Consent management ----
    /** Consent to process personal/health data (required for the platform to operate). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean dataProcessingConsent = false;

    /** Opt-in to email notifications (marketing / newsletters). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean emailNotifications = true;

    // ---- Privacy settings ----
    /** Whether the user's profile is visible to healthcare professionals. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean profileVisible = false;

    /** Whether doctors may view this user's health profile during consultations. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean shareHealthDataWithDoctors = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
