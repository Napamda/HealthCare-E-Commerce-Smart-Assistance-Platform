package org.example.Healthcareplatform.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.example.Healthcareplatform.user.dto.*;
import org.example.Healthcareplatform.user.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * User management endpoints. All routes operate on the authenticated
 * user derived from the JWT — no client-supplied userIds.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final SecurityContextUtil securityContextUtil;

    // ---- Task 2.1 — Profile ----

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(userProfileService.getProfile(securityContextUtil.getCurrentUserId()));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = securityContextUtil.getCurrentUserId();
        try {
            return ResponseEntity.ok(userProfileService.updateProfile(userId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = securityContextUtil.getCurrentUserId();
        try {
            return ResponseEntity.ok(userProfileService.uploadAvatar(userId, file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Avatar upload failed — userId={}", userId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Avatar upload failed"));
        }
    }

    // ---- Task 2.2 — Health Profile ----

    @GetMapping("/me/health-profile")
    public ResponseEntity<HealthProfileResponse> getHealthProfile() {
        return ResponseEntity.ok(userProfileService.getHealthProfile(securityContextUtil.getCurrentUserId()));
    }

    @PutMapping("/me/health-profile")
    public ResponseEntity<?> updateHealthProfile(@Valid @RequestBody UpdateHealthProfileRequest request) {
        Long userId = securityContextUtil.getCurrentUserId();
        try {
            return ResponseEntity.ok(userProfileService.updateHealthProfile(userId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---- Task 2.3 — Address Management ----

    @GetMapping("/me/addresses")
    public ResponseEntity<List<AddressResponse>> listAddresses() {
        return ResponseEntity.ok(userProfileService.listAddresses(securityContextUtil.getCurrentUserId()));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<?> addAddress(@Valid @RequestBody AddressRequest request) {
        Long userId = securityContextUtil.getCurrentUserId();
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(userProfileService.addAddress(userId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/me/addresses/{id}")
    public ResponseEntity<?> updateAddress(@PathVariable Long id,
                                           @Valid @RequestBody AddressRequest request) {
        Long userId = securityContextUtil.getCurrentUserId();
        try {
            return ResponseEntity.ok(userProfileService.updateAddress(userId, id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/me/addresses/{id}")
    public ResponseEntity<?> deleteAddress(@PathVariable Long id) {
        Long userId = securityContextUtil.getCurrentUserId();
        try {
            userProfileService.deleteAddress(userId, id);
            return ResponseEntity.ok(Map.of("message", "Address deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/me/addresses/{id}/default")
    public ResponseEntity<?> setDefaultAddress(@PathVariable Long id) {
        Long userId = securityContextUtil.getCurrentUserId();
        try {
            return ResponseEntity.ok(userProfileService.setDefaultAddress(userId, id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
