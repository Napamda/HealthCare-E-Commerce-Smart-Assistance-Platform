package org.example.Healthcareplatform.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessionalProfileResponse {

    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String title;
    private String specialty;
    private String bio;
    private String phone;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
