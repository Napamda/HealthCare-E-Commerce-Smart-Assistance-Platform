package org.example.Healthcareplatform.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.user.dto.UserProfileRequest;
import org.example.Healthcareplatform.user.dto.UserProfileResponse;
import org.example.Healthcareplatform.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        log.info("GET /api/users/profile — userId={}", userId);
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestBody UserProfileRequest request,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        log.info("PUT /api/users/profile — userId={}", userId);
        return ResponseEntity.ok(userService.updateUserProfile(userId, request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        log.info("POST /api/users/change-password — userId={}", userId);
        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}