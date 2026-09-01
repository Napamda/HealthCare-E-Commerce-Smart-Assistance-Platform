package org.example.Healthcareplatform.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessionalProfileRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String title;

    @NotBlank
    private String specialty;

    private String bio;

    private String phone;

    private String address;

    private String city;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @Builder.Default
    private boolean active = true;
}
