package org.example.Healthcareplatform.pharmacist.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.example.Healthcareplatform.pharmacist.dto.PharmacistProfileRequest;
import org.example.Healthcareplatform.pharmacist.dto.PharmacistProfileResponse;
import org.example.Healthcareplatform.pharmacist.service.PharmacistProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pharmacist/profile")
@RequiredArgsConstructor
@Slf4j
public class PharmacistProfileController {

    private final PharmacistProfileService pharmacistProfileService;
    private final SecurityContextUtil securityContextUtil;

    @GetMapping
    public ResponseEntity<PharmacistProfileResponse> getProfile() {
        Long userId = securityContextUtil.getCurrentUserId();
        return ResponseEntity.ok(pharmacistProfileService.getProfile(userId));
    }

    @PostMapping
    public ResponseEntity<?> createProfile(@Valid @RequestBody PharmacistProfileRequest request) {
        Long userId = securityContextUtil.getCurrentUserId();
        request.setUserId(userId);
        try {
            PharmacistProfileResponse response = pharmacistProfileService.createProfile(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@Valid @RequestBody PharmacistProfileRequest request) {
        Long userId = securityContextUtil.getCurrentUserId();
        request.setUserId(userId);
        try {
            return ResponseEntity.ok(pharmacistProfileService.updateProfile(userId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}