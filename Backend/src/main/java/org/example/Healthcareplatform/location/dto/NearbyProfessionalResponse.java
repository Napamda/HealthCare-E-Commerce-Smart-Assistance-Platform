package org.example.Healthcareplatform.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyProfessionalResponse {

    private ProfessionalProfileResponse profile;
    private double distanceKm;
}
