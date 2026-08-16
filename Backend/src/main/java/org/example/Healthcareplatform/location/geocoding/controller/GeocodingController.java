package org.example.Healthcareplatform.location.geocoding.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.location.geocoding.dto.GeocodeResult;
import org.example.Healthcareplatform.location.geocoding.service.GeocodingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/geocode")
@RequiredArgsConstructor
@Slf4j
public class GeocodingController {

    private final GeocodingService geocodingService;

    @GetMapping
    public ResponseEntity<GeocodeResult> geocodeAddress(@RequestParam String address) {
        log.info("Geocode address — address={}", address);
        return ResponseEntity.ok(geocodingService.geocode(address));
    }

    @GetMapping("/reverse")
    public ResponseEntity<GeocodeResult> reverseGeocode(@RequestParam double lat,
                                                        @RequestParam double lng) {
        log.info("Reverse geocode — lat={}, lng={}", lat, lng);
        return ResponseEntity.ok(geocodingService.reverseGeocode(lat, lng));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(IllegalArgumentException e) {
        log.warn("Geocoding validation error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeError(RuntimeException e) {
        log.error("Geocoding error: {}", e.getMessage(), e);
        if (e.getMessage() != null && e.getMessage().contains("No location found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
    }
}
