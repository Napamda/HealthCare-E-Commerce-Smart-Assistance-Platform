package org.example.Healthcareplatform.doctor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.example.Healthcareplatform.doctor.dto.DoctorProfileRequest;
import org.example.Healthcareplatform.doctor.dto.DoctorProfileResponse;
import org.example.Healthcareplatform.doctor.service.DoctorProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/doctor/profile")
@RequiredArgsConstructor
@Slf4j
public class DoctorProfileController {

    private final DoctorProfileService doctorProfileService;
    private final SecurityContextUtil securityContextUtil;

    @GetMapping
    public ResponseEntity<DoctorProfileResponse> getProfile() {
        Long userId = securityContextUtil.getCurrentUserId();
        return ResponseEntity.ok(doctorProfileService.getProfile(userId));
    }

    @PostMapping
    public ResponseEntity<?> createProfile(@Valid @RequestBody DoctorProfileRequest request) {
        Long userId = securityContextUtil.getCurrentUserId();
        request.setUserId(userId);
        try {
            DoctorProfileResponse response = doctorProfileService.createProfile(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@Valid @RequestBody DoctorProfileRequest request) {
        Long userId = securityContextUtil.getCurrentUserId();
        request.setUserId(userId);
        try {
            return ResponseEntity.ok(doctorProfileService.updateProfile(userId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}