package org.example.Healthcareplatform.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfileResponse {

    private Long id;

    private Long userId;

    private String clinicName;

    private String clinicAddress;

    private String clinicCity;

    private String clinicState;

    private String clinicPostalCode;

    private String clinicCountry;

    private String medicalLicenseNumber;

    private String licenseState;

    private String licenseExpirationDate;

    private String specialty;

    private String subSpecialty;

    private String bio;

    private String consultationFee;

    private String clinicPhone;

    private String clinicEmail;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}