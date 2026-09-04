package org.example.Healthcareplatform.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.user.dto.UserProfileRequest;
import org.example.Healthcareplatform.user.dto.UserProfileResponse;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhone(user.getPhone());
        response.setDateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null);
        response.setRole(user.getRole().name());
        
        return response;
    }

    @Transactional
    public UserProfileResponse updateUserProfile(Long userId, UserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
            try {
                user.setDateOfBirth(java.time.LocalDate.parse(request.getDateOfBirth()));
            } catch (Exception e) {
                log.warn("Invalid date format: {}", request.getDateOfBirth());
            }
        }

        User saved = userRepository.save(user);
        log.info("Updated user profile for userId={}", userId);
        
        return getUserProfile(saved.getId());
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Changed password for userId={}", userId);
    }
}