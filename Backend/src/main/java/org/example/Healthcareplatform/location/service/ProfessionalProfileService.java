package org.example.Healthcareplatform.location.service;

import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.location.dto.NearbyProfessionalResponse;
import org.example.Healthcareplatform.location.dto.ProfessionalProfileRequest;
import org.example.Healthcareplatform.location.dto.ProfessionalProfileResponse;
import org.example.Healthcareplatform.location.entity.ProfessionalProfile;
import org.example.Healthcareplatform.location.repository.ProfessionalProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class ProfessionalProfileService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final ProfessionalProfileRepository profileRepository;

    public ProfessionalProfileService(ProfessionalProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
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
        List<ProfessionalProfile> candidates = (specialty == null || specialty.isBlank())
                ? profileRepository.findByActiveTrueAndLatitudeIsNotNullAndLongitudeIsNotNull()
                : profileRepository.findBySpecialtyContainingIgnoreCaseAndActiveTrueOrderByLastNameAsc(specialty)
                        .stream()
                        .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                        .toList();

        return candidates.stream()
                .map(p -> new NearbyProfessionalResponse(toResponse(p),
                        haversineKm(latitude, longitude, p.getLatitude(), p.getLongitude())))
                .filter(n -> n.getDistanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyProfessionalResponse::getDistanceKm))
                .toList();
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
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
