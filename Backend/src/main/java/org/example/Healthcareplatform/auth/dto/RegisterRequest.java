package org.example.Healthcareplatform.auth.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import jakarta.validation.constraints.Pattern;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
    )
    private String password;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;

    @NotBlank(message = "Role is required")
    @Pattern(
        regexp = "^(?i)(PATIENT|PHARMACIST|DOCTOR|ADMIN|VENDOR)$",
        message = "Role must be one of: PATIENT, PHARMACIST, DOCTOR, ADMIN, VENDOR"
    )
    private String role;
}
