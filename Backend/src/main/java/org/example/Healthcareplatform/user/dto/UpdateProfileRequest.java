package org.example.Healthcareplatform.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Editable profile fields. Names are required and must not be blank;
 * everything else is optional. Email and role are intentionally NOT
 * editable here.
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9 ()\\-]{7,20}$", message = "Phone must be 7-20 digits, optionally starting with +")
    private String phone;

    private LocalDate dateOfBirth;

    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Gender must be MALE, FEMALE or OTHER")
    private String gender;
}
