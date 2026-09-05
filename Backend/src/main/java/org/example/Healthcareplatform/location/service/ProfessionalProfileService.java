package org.example.Healthcareplatform.location.service;

import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.location.dto.NearbyProfessionalResponse;
import org.example.Healthcareplatform.location.dto.NearbyProfessionalRow;
import org.example.Healthcareplatform.location.dto.ProfessionalProfileRequest;
import org.example.Healthcareplatform.location.dto.ProfessionalProfileResponse;
import org.example.Healthcareplatform.location.entity.ProfessionalProfile;
import org.example.Healthcareplatform.location.mapper.ProfessionalProfileGeoMapper;
import org.example.Healthcareplatform.location.repository.ProfessionalProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ProfessionalProfileService {

    private final ProfessionalProfileRepository profileRepository;
    private final ProfessionalProfileGeoMapper professionalProfileGeoMapper;

    public ProfessionalProfileService(ProfessionalProfileRepository profileRepository,
                                      ProfessionalProfileGeoMapper professionalProfileGeoMapper) {
        this.profileRepository = profileRepository;
        this.professionalProfileGeoMapper = professionalProfileGeoMapper;
    }

    @Transactional(readOnly = true)
    public List<ProfessionalProfileResponse> listProfiles(String specialty) {
        List<ProfessionalProfile> profiles = (specialty == null || specialty.isBlank())
                ? profileRepository.findByActiveTrueOrderByLastNameAsc()
                : profileRepository.findBySpecialtyContainingIgnoreCaseAndActiveTrueOrderByLastNameAsc(specialty);
        return profiles.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<String> listSpecialties() {
        return profileRepository.findDistinctSpecialtyByActiveTrueOrderBySpecialtyAsc();
    }

    @Transactional(readOnly = true)
    public ProfessionalProfileResponse getProfile(Long id) {
        ProfessionalProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Professional profile not found with id: " + id));
        return toResponse(profile);
    }

    @Transactional
    public ProfessionalProfileResponse createProfile(ProfessionalProfileRequest request) {
        profileRepository.findByUserId(request.getUserId()).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "A professional profile already exists for user id: " + request.getUserId());
        });

        ProfessionalProfile profile = ProfessionalProfile.builder()
                .userId(request.getUserId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .title(request.getTitle())
                .specialty(request.getSpecialty())
                .bio(request.getBio())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .active(request.isActive())
                .build();

        profile = profileRepository.save(profile);
        log.info("Professional profile created — id={}, userId={}, specialty={}",
                profile.getId(), profile.getUserId(), profile.getSpecialty());
        return toResponse(profile);
    }

    @Transactional
    public ProfessionalProfileResponse updateProfile(Long id, ProfessionalProfileRequest request) {
        ProfessionalProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Professional profile not found with id: " + id));

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setTitle(request.getTitle());
        profile.setSpecialty(request.getSpecialty());
        profile.setBio(request.getBio());
        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        profile.setLatitude(request.getLatitude());
        profile.setLongitude(request.getLongitude());
        profile.setActive(request.isActive());

        profile = profileRepository.save(profile);
        log.info("Professional profile updated — id={}", id);
        return toResponse(profile);
    }

    @Transactional
    public void deleteProfile(Long id) {
        ProfessionalProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Professional profile not found with id: " + id));
        profileRepository.delete(profile);
        log.info("Professional profile deleted — id={}", id);
    }

    @Transactional(readOnly = true)
    public List<NearbyProfessionalResponse> searchNearby(double latitude, double longitude,
                                                         double radiusKm, String specialty) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("Search radius must be greater than 0");
        }

        String normalizedSpecialty = (specialty == null || specialty.isBlank()) ? null : specialty;
        return professionalProfileGeoMapper
                .findNearby(latitude, longitude, radiusKm, normalizedSpecialty)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private NearbyProfessionalResponse toResponse(NearbyProfessionalRow row) {
        ProfessionalProfileResponse profile = ProfessionalProfileResponse.builder()
                .id(row.getId())
                .userId(row.getUserId())
                .firstName(row.getFirstName())
                .lastName(row.getLastName())
                .fullName(row.getFirstName() + " " + row.getLastName())
                .title(row.getTitle())
                .specialty(row.getSpecialty())
                .bio(row.getBio())
                .phone(row.getPhone())
                .address(row.getAddress())
                .city(row.getCity())
                .latitude(row.getLatitude())
                .longitude(row.getLongitude())
                .active(row.isActive())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
        return NearbyProfessionalResponse.builder()
                .profile(profile)
                .distanceKm(row.getDistanceKm())
                .build();
    }

    private ProfessionalProfileResponse toResponse(ProfessionalProfile p) {
        return ProfessionalProfileResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .fullName(p.getFirstName() + " " + p.getLastName())
                .title(p.getTitle())
                .specialty(p.getSpecialty())
                .bio(p.getBio())
                .phone(p.getPhone())
                .address(p.getAddress())
                .city(p.getCity())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
