package org.example.Healthcareplatform.location.geocoding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeocodeResult {

    private double latitude;
    private double longitude;
    private String displayName;
    private String city;
    private String country;
}
