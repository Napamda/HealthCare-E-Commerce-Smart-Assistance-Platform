package org.example.Healthcareplatform.pharmacist.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.pharmacist.dto.PharmacistProfileRequest;
import org.example.Healthcareplatform.pharmacist.dto.PharmacistProfileResponse;
import org.example.Healthcareplatform.pharmacist.entity.PharmacistProfile;
import org.example.Healthcareplatform.pharmacist.repository.PharmacistProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacistProfileService {

    private final PharmacistProfileRepository pharmacistProfileRepository;

    @Transactional(readOnly = true)
    public PharmacistProfileResponse getProfile(Long userId) {
        return pharmacistProfileRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(this::defaultResponse);
    }

    @Transactional
    public PharmacistProfileResponse createProfile(PharmacistProfileRequest request) {
        pharmacistProfileRepository.findByUserId(request.getUserId()).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "A pharmacist profile already exists for user id: " + request.getUserId());
        });

        PharmacistProfile profile = PharmacistProfile.builder()
                .userId(request.getUserId())
                .pharmacyName(request.getPharmacyName())
                .pharmacyAddress(request.getPharmacyAddress())
                .pharmacyCity(request.getPharmacyCity())
                .pharmacyState(request.getPharmacyState())
                .pharmacyPostalCode(request.getPharmacyPostalCode())
                .pharmacyCountry(request.getPharmacyCountry())
                .licenseNumber(request.getLicenseNumber())
                .licenseState(request.getLicenseState())
                .licenseExpirationDate(request.getLicenseExpirationDate())
                .pharmacyPhone(request.getPharmacyPhone())
                .pharmacyEmail(request.getPharmacyEmail())
                .bio(request.getBio())
                .specialization(request.getSpecialization())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        profile = pharmacistProfileRepository.save(profile);
        log.info("Pharmacist profile created — id={}, userId={}, pharmacy={}",
                profile.getId(), profile.getUserId(), profile.getPharmacyName());
        return toResponse(profile);
    }

    @Transactional
    public PharmacistProfileResponse updateProfile(Long userId, PharmacistProfileRequest request) {
        PharmacistProfile profile = pharmacistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Pharmacist profile not found for user id: " + userId));

        profile.setPharmacyName(request.getPharmacyName());
        profile.setPharmacyAddress(request.getPharmacyAddress());
        profile.setPharmacyCity(request.getPharmacyCity());
        profile.setPharmacyState(request.getPharmacyState());
        profile.setPharmacyPostalCode(request.getPharmacyPostalCode());
        profile.setPharmacyCountry(request.getPharmacyCountry());
        profile.setLicenseNumber(request.getLicenseNumber());
        profile.setLicenseState(request.getLicenseState());
        profile.setLicenseExpirationDate(request.getLicenseExpirationDate());
        profile.setPharmacyPhone(request.getPharmacyPhone());
        profile.setPharmacyEmail(request.getPharmacyEmail());
        profile.setBio(request.getBio());
        profile.setSpecialization(request.getSpecialization());
        if (request.getActive() != null) {
            profile.setActive(request.getActive());
        }

        profile = pharmacistProfileRepository.save(profile);
        log.info("Pharmacist profile updated — userId={}", userId);
        return toResponse(profile);
    }

    private PharmacistProfileResponse toResponse(PharmacistProfile profile) {
        return PharmacistProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .pharmacyName(profile.getPharmacyName())
                .pharmacyAddress(profile.getPharmacyAddress())
                .pharmacyCity(profile.getPharmacyCity())
                .pharmacyState(profile.getPharmacyState())
                .pharmacyPostalCode(profile.getPharmacyPostalCode())
                .pharmacyCountry(profile.getPharmacyCountry())
                .licenseNumber(profile.getLicenseNumber())
                .licenseState(profile.getLicenseState())
                .licenseExpirationDate(profile.getLicenseExpirationDate())
                .pharmacyPhone(profile.getPharmacyPhone())
                .pharmacyEmail(profile.getPharmacyEmail())
                .bio(profile.getBio())
                .specialization(profile.getSpecialization())
                .active(profile.isActive())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private PharmacistProfileResponse defaultResponse() {
        return PharmacistProfileResponse.builder()
                .pharmacyName("")
                .pharmacyAddress("")
                .pharmacyCity("")
                .pharmacyState("")
                .pharmacyPostalCode("")
                .pharmacyCountry("")
                .licenseNumber("")
                .licenseState("")
                .licenseExpirationDate("")
                .pharmacyPhone("")
                .pharmacyEmail("")
                .bio("")
                .specialization("")
                .active(false)
                .build();
    }
}