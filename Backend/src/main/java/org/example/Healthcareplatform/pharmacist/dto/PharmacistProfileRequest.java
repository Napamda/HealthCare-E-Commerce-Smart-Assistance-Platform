package org.example.Healthcareplatform.pharmacist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacistProfileRequest {

    private Long userId;

    private String pharmacyName;

    private String pharmacyAddress;

    private String pharmacyCity;

    private String pharmacyState;

    private String pharmacyPostalCode;

    private String pharmacyCountry;

    private String licenseNumber;

    private String licenseState;

    private String licenseExpirationDate;

    private String pharmacyPhone;

    private String pharmacyEmail;

    private String bio;

    private String specialization;

    private Boolean active;
}