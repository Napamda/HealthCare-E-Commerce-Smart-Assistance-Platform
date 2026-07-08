package org.example.Healthcareplatform.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.auth.dto.*;
import org.example.Healthcareplatform.auth.util.JwtUtil;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.entity.UserRole;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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
                    "Invalid role: " + request.getRole() + ". Must be PATIENT, PHARMACIST, or DOCTOR");
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(role)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .build();

        user = userRepository.save(user);
        log.info("User registered — id={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

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

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), request.isRememberMe());

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

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
