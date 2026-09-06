package org.example.Healthcareplatform.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.admin.entity.UserActivityLog;
import org.example.Healthcareplatform.admin.entity.VendorProfile;
import org.example.Healthcareplatform.admin.repository.VendorProfileRepository;
import org.example.Healthcareplatform.admin.service.ActivityLogService;
import org.example.Healthcareplatform.auth.dto.*;
import org.example.Healthcareplatform.auth.util.JwtUtil;
import org.example.Healthcareplatform.doctor.entity.DoctorProfile;
import org.example.Healthcareplatform.doctor.repository.DoctorProfileRepository;
import org.example.Healthcareplatform.messaging.publisher.HealthcareEventPublisher;
import org.example.Healthcareplatform.notification.service.EmailNotificationService;
import org.example.Healthcareplatform.pharmacist.entity.PharmacistProfile;
import org.example.Healthcareplatform.pharmacist.repository.PharmacistProfileRepository;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.entity.UserRole;
import org.example.Healthcareplatform.user.entity.UserStatus;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailNotificationService emailNotificationService;
    private final HealthcareEventPublisher eventPublisher;
    private final ActivityLogService activityLogService;
    private final VendorProfileRepository vendorProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PharmacistProfileRepository pharmacistProfileRepository;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid role: " + request.getRole() + ". Must be PATIENT, PHARMACIST, DOCTOR, or VENDOR");
        }

        validateRoleDetails(request, role);

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(role)
                .dateOfBirth(parseDate(request.getDateOfBirth()))
                .emailVerified(false)
                .verificationToken(verificationToken)
                .build();

        user = userRepository.save(user);
        log.info("User registered — id={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

        // Send welcome email (async, non-blocking)
        String dashboardUrl = "http://localhost:5173/products";
        String fullName = user.getFirstName() + " " + user.getLastName();
        try {
            emailNotificationService.sendWelcomeEmail(user.getEmail(), fullName, dashboardUrl);
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }

        // Publish user.registered event to RabbitMQ
        eventPublisher.publishUserRegistered(user.getId(), user.getEmail(), fullName);

        // Task 3.1 — activity audit trail
        activityLogService.record(user.getId(), null, UserActivityLog.ActivityAction.REGISTERED,
                "Account created with role " + role.name());

        if (role == UserRole.DOCTOR) {
            doctorProfileRepository.save(DoctorProfile.builder()
                    .userId(user.getId())
                    .clinicName(required(request.getClinicName(), "Clinic name is required for doctors"))
                    .medicalLicenseNumber(request.getMedicalLicenseNumber())
                    .specialty(request.getSpecialty())
                    .build());
        }

        if (role == UserRole.PHARMACIST) {
            pharmacistProfileRepository.save(PharmacistProfile.builder()
                    .userId(user.getId())
                    .pharmacyName(required(request.getPharmacyName(), "Pharmacy name is required for pharmacists"))
                    .licenseNumber(request.getPharmacistLicenseNumber())
                    .specialization(request.getSpecialization())
                    .build());
        }

        // Task 3.2 — create pending vendor application when role is VENDOR
        if (role == UserRole.VENDOR) {
            String bizName = required(request.getBusinessName(), "Business name is required for vendors");
            vendorProfileRepository.save(VendorProfile.builder()
                    .userId(user.getId())
                    .businessName(bizName)
                    .businessLicense(request.getBusinessLicense())
                    .approvalStatus(VendorProfile.ApprovalStatus.PENDING)
                    .build());
            log.info("Vendor application created — userId={}, businessName={}", user.getId(), bizName);
        }

        String verificationLink = "http://localhost:8080/api/auth/verify-email?token=" + verificationToken;
        log.info("Verification link for user {}: {}", user.getEmail(), verificationLink);

        return RegisterResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .message("Registration successful. Please check your email to verify your account.")
                .verificationLink(verificationLink)
                .build();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Date of birth must use YYYY-MM-DD format");
        }
    }

    private void validateRoleDetails(RegisterRequest request, UserRole role) {
        if (role == UserRole.DOCTOR) {
            required(request.getClinicName(), "Clinic name is required for doctors");
        } else if (role == UserRole.PHARMACIST) {
            required(request.getPharmacyName(), "Pharmacy name is required for pharmacists");
        } else if (role == UserRole.VENDOR) {
            required(request.getBusinessName(), "Business name is required for vendors");
        }
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("Please verify your email before logging in");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalArgumentException(
                    "Your account has been suspended. Please contact support for assistance.");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), request.isRememberMe());

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        activityLogService.record(user.getId(), null, UserActivityLog.ActivityAction.LOGIN,
                "User logged in");

        long expirationMs = request.isRememberMe()
                ? 2592000000L
                : 86400000L;

        log.info("User logged in — id={}, email={}, rememberMe={}", user.getId(), user.getEmail(), request.isRememberMe());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(900000L)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .rememberMe(request.isRememberMe())
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        Claims claims;
        try {
            claims = jwtUtil.validateToken(request.getRefreshToken());
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String tokenType = claims.get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new BadCredentialsException("Invalid token type — expected refresh token");
        }

        Long userId = Long.parseLong(claims.getSubject());
        boolean rememberMe = claims.get("rememberMe", Boolean.class);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!request.getRefreshToken().equals(user.getRefreshToken())) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            // Suspended accounts keep no valid session — force a fresh login attempt.
            user.setRefreshToken(null);
            userRepository.save(user);
            throw new BadCredentialsException("Account suspended");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), rememberMe);

        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        log.info("Token refreshed — userId={}, rememberMe={}", user.getId(), rememberMe);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresInMs(900000L)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .rememberMe(rememberMe)
                .build();
    }

    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setRefreshToken(null);
            userRepository.save(user);
            log.info("User logged out — id={}", userId);
        });
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification token"));

        if (user.isEmailVerified()) {
            log.info("Email already verified for user — id={}", user.getId());
            return;
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
        log.info("Email verified for user — id={}", user.getId());
    }
}
