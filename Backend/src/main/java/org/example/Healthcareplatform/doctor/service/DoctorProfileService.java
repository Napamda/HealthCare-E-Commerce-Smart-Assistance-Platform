package org.example.Healthcareplatform.doctor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.doctor.dto.DoctorProfileRequest;
import org.example.Healthcareplatform.doctor.dto.DoctorProfileResponse;
import org.example.Healthcareplatform.doctor.entity.DoctorProfile;
import org.example.Healthcareplatform.doctor.repository.DoctorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorProfileService {

    private final DoctorProfileRepository doctorProfileRepository;

    @Transactional(readOnly = true)
    public DoctorProfileResponse getProfile(Long userId) {
        return doctorProfileRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(this::defaultResponse);
    }

    @Transactional
    public DoctorProfileResponse createProfile(DoctorProfileRequest request) {
        doctorProfileRepository.findByUserId(request.getUserId()).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "A doctor profile already exists for user id: " + request.getUserId());
        });

        DoctorProfile profile = DoctorProfile.builder()
                .userId(request.getUserId())
                .clinicName(request.getClinicName())
                .clinicAddress(request.getClinicAddress())
                .clinicCity(request.getClinicCity())
                .clinicState(request.getClinicState())
                .clinicPostalCode(request.getClinicPostalCode())
                .clinicCountry(request.getClinicCountry())
                .medicalLicenseNumber(request.getMedicalLicenseNumber())
                .licenseState(request.getLicenseState())
                .licenseExpirationDate(request.getLicenseExpirationDate())
                .specialty(request.getSpecialty())
                .subSpecialty(request.getSubSpecialty())
                .bio(request.getBio())
                .consultationFee(request.getConsultationFee())
                .clinicPhone(request.getClinicPhone())
                .clinicEmail(request.getClinicEmail())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        profile = doctorProfileRepository.save(profile);
        log.info("Doctor profile created — id={}, userId={}, specialty={}",
                profile.getId(), profile.getUserId(), profile.getSpecialty());
        return toResponse(profile);
    }

    @Transactional
    public DoctorProfileResponse updateProfile(Long userId, DoctorProfileRequest request) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found for user id: " + userId));

        profile.setClinicName(request.getClinicName());
        profile.setClinicAddress(request.getClinicAddress());
        profile.setClinicCity(request.getClinicCity());
        profile.setClinicState(request.getClinicState());
        profile.setClinicPostalCode(request.getClinicPostalCode());
        profile.setClinicCountry(request.getClinicCountry());
        profile.setMedicalLicenseNumber(request.getMedicalLicenseNumber());
        profile.setLicenseState(request.getLicenseState());
        profile.setLicenseExpirationDate(request.getLicenseExpirationDate());
        profile.setSpecialty(request.getSpecialty());
        profile.setSubSpecialty(request.getSubSpecialty());
        profile.setBio(request.getBio());
        profile.setConsultationFee(request.getConsultationFee());
        profile.setClinicPhone(request.getClinicPhone());
        profile.setClinicEmail(request.getClinicEmail());
        if (request.getActive() != null) {
            profile.setActive(request.getActive());
        }

        profile = doctorProfileRepository.save(profile);
        log.info("Doctor profile updated — userId={}", userId);
        return toResponse(profile);
    }

    private DoctorProfileResponse toResponse(DoctorProfile profile) {
        return DoctorProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .clinicName(profile.getClinicName())
                .clinicAddress(profile.getClinicAddress())
                .clinicCity(profile.getClinicCity())
                .clinicState(profile.getClinicState())
                .clinicPostalCode(profile.getClinicPostalCode())
                .clinicCountry(profile.getClinicCountry())
                .medicalLicenseNumber(profile.getMedicalLicenseNumber())
                .licenseState(profile.getLicenseState())
                .licenseExpirationDate(profile.getLicenseExpirationDate())
                .specialty(profile.getSpecialty())
                .subSpecialty(profile.getSubSpecialty())
                .bio(profile.getBio())
                .consultationFee(profile.getConsultationFee())
                .clinicPhone(profile.getClinicPhone())
                .clinicEmail(profile.getClinicEmail())
                .active(profile.isActive())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private DoctorProfileResponse defaultResponse() {
        return DoctorProfileResponse.builder()
                .clinicName("")
                .clinicAddress("")
                .clinicCity("")
                .clinicState("")
                .clinicPostalCode("")
                .clinicCountry("")
                .medicalLicenseNumber("")
                .licenseState("")
                .licenseExpirationDate("")
                .specialty("")
                .subSpecialty("")
                .bio("")
                .consultationFee("")
                .clinicPhone("")
                .clinicEmail("")
                .active(false)
                .build();
    }
}