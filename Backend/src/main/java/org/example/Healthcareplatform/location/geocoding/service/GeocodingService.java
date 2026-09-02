package org.example.Healthcareplatform.location.geocoding.service;

import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.location.geocoding.dto.GeocodeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeocodingService {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> SEARCH_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public GeocodingService(
            @Value("${geocoding.base-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${geocoding.user-agent:HealthCarePlatform/1.0 (https://localhost)}") String userAgent) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
        log.info("GeocodingService initialized — baseUrl={}", baseUrl);
    }

    @Cacheable(cacheNames = "geocode")
    public GeocodeResult geocode(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Address query is required");
        }
        log.info("Geocoding address — query={}", query);

        List<Map<String, Object>> results = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("limit", 1)
                        .queryParam("addressdetails", 1)
                        .build())
                .retrieve()
                .body(SEARCH_TYPE);

        if (results == null || results.isEmpty()) {
            throw new RuntimeException("No location found for address: " + query);
        }
        return parseResult(results.get(0));
    }

    @Cacheable(cacheNames = "reverseGeocode")
    public GeocodeResult reverseGeocode(double latitude, double longitude) {
        log.info("Reverse geocoding — lat={}, lng={}", latitude, longitude);

        Map<String, Object> result = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/reverse")
                        .queryParam("lat", latitude)
                        .queryParam("lon", longitude)
                        .queryParam("format", "json")
                        .queryParam("addressdetails", 1)
                        .build())
                .retrieve()
                .body(Map.class);

        if (result == null || result.isEmpty()) {
            throw new RuntimeException("No address found for coordinates");
        }
        return parseResult(result);
    }

    private GeocodeResult parseResult(Map<String, Object> item) {
        double lat = Double.parseDouble(String.valueOf(item.get("lat")));
        double lng = Double.parseDouble(String.valueOf(item.get("lon")));
        String displayName = item.get("display_name") == null
                ? null : String.valueOf(item.get("display_name"));

        String city = null;
        String country = null;
        if (item.get("address") instanceof Map<?, ?> address) {
            city = firstOf(address, "city", "town", "village", "county", "state");
            country = firstOf(address, "country");
        }
        return GeocodeResult.builder()
                .latitude(lat)
                .longitude(lng)
                .displayName(displayName)
                .city(city)
                .country(country)
                .build();
    }

    private String firstOf(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}
