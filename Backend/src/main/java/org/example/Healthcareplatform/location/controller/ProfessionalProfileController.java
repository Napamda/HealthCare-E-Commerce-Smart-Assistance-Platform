package org.example.Healthcareplatform.location.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.location.dto.NearbyProfessionalResponse;
import org.example.Healthcareplatform.location.dto.ProfessionalProfileRequest;
import org.example.Healthcareplatform.location.dto.ProfessionalProfileResponse;
import org.example.Healthcareplatform.location.service.ProfessionalProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
@Slf4j
public class ProfessionalProfileController {

    private final ProfessionalProfileService professionalProfileService;

    @GetMapping
    public ResponseEntity<List<ProfessionalProfileResponse>> listProfiles(
            @RequestParam(required = false) String specialty) {
        log.info("List professional profiles — specialty={}", specialty);
        return ResponseEntity.ok(professionalProfileService.listProfiles(specialty));
    }

    @GetMapping("/specialties")
    public ResponseEntity<List<String>> listSpecialties() {
        log.info("List professional specialties");
        return ResponseEntity.ok(professionalProfileService.listSpecialties());
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyProfessionalResponse>> searchNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "25") double radius,
            @RequestParam(required = false) String specialty) {
        log.info("Search nearby professionals — lat={}, lng={}, radiusKm={}, specialty={}",
                lat, lng, radius, specialty);
        return ResponseEntity.ok(
                professionalProfileService.searchNearby(lat, lng, radius, specialty));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfessionalProfileResponse> getProfile(@PathVariable Long id) {
        log.info("Get professional profile — id={}", id);
        return ResponseEntity.ok(professionalProfileService.getProfile(id));
    }

    @PostMapping
    public ResponseEntity<ProfessionalProfileResponse> createProfile(
            @Valid @RequestBody ProfessionalProfileRequest request) {
        log.info("Create professional profile — userId={}", request.getUserId());
        ProfessionalProfileResponse response = professionalProfileService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfessionalProfileResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody ProfessionalProfileRequest request) {
        log.info("Update professional profile — id={}", id);
        return ResponseEntity.ok(professionalProfileService.updateProfile(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        log.info("Delete professional profile — id={}", id);
        professionalProfileService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }

}
