package org.example.Healthcareplatform.pharmacist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Pharmacist-specific profile containing pharmacy practice information,
 * credentials, and operational details.
 */
@Entity
@Table(name = "pharmacist_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String pharmacyName;

    @Column(length = 255)
    private String pharmacyAddress;

    @Column(length = 100)
    private String pharmacyCity;

    @Column(length = 100)
    private String pharmacyState;

    @Column(length = 20)
    private String pharmacyPostalCode;

    @Column(length = 100)
    private String pharmacyCountry;

    @Column(length = 50)
    private String licenseNumber;

    @Column(length = 20)
    private String licenseState;

    @Column(length = 20)
    private String licenseExpirationDate;

    @Column(length = 255)
    private String pharmacyPhone;

    @Column(length = 255)
    private String pharmacyEmail;

    @Column(length = 1000)
    private String bio;

    @Column(length = 100)
    private String specialization;

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