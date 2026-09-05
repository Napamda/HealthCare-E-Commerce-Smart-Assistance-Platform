package org.example.Healthcareplatform.doctor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Doctor-specific profile containing medical practice information,
 * credentials, and specialization details.
 */
@Entity
@Table(name = "doctor_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String clinicName;

    @Column(length = 255)
    private String clinicAddress;

    @Column(length = 100)
    private String clinicCity;

    @Column(length = 100)
    private String clinicState;

    @Column(length = 20)
    private String clinicPostalCode;

    @Column(length = 100)
    private String clinicCountry;

    @Column(length = 50)
    private String medicalLicenseNumber;

    @Column(length = 20)
    private String licenseState;

    @Column(length = 20)
    private String licenseExpirationDate;

    @Column(length = 100)
    private String specialty;

    @Column(length = 100)
    private String subSpecialty;

    @Column(length = 1000)
    private String bio;

    @Column(length = 50)
    private String consultationFee;

    @Column(length = 255)
    private String clinicPhone;

    @Column(length = 255)
    private String clinicEmail;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}