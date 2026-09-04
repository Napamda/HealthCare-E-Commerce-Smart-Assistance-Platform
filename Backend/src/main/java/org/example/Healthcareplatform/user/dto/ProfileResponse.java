package org.example.Healthcareplatform.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The authenticated user's own profile (view + edit response).
 */
@Data
@Builder
public class ProfileResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String gender;
    private boolean emailVerified;
    private LocalDateTime createdAt;
}
