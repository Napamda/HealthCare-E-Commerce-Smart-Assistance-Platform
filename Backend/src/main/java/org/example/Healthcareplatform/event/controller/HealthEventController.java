package org.example.Healthcareplatform.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.event.dto.HealthEventRequest;
import org.example.Healthcareplatform.event.dto.HealthEventResponse;
import org.example.Healthcareplatform.event.service.HealthEventService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class HealthEventController {

    private final HealthEventService eventService;

    @GetMapping
    public ResponseEntity<List<HealthEventResponse>> listEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        log.info("List events — category={}, status={}", category, status);
        return ResponseEntity.ok(eventService.listEvents(category, status));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> listCategories() {
        log.info("List event categories");
        return ResponseEntity.ok(eventService.listCategories());
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> listCities() {
        log.info("List event cities");
        return ResponseEntity.ok(eventService.listCities());
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        log.info("Search events — keyword={}, category={}, city={}, dateFrom={}, dateTo={}, status={}, page={}, size={}",
                keyword, category, city, dateFrom, dateTo, status, page, size);
        Page<HealthEventResponse> result = eventService.searchEvents(
                keyword, category, city, dateFrom, dateTo, status, page, size);
        return ResponseEntity.ok(Map.of(
                "content", result.getContent(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "first", result.isFirst(),
                "last", result.isLast()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HealthEventResponse> getEvent(@PathVariable Long id) {
        log.info("Get event — id={}", id);
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    @PostMapping
    public ResponseEntity<HealthEventResponse> createEvent(@RequestBody HealthEventRequest request) {
        log.info("Create event — title={}", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HealthEventResponse> updateEvent(@PathVariable Long id,
                                                           @RequestBody HealthEventRequest request) {
        log.info("Update event — id={}", id);
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteEvent(@PathVariable Long id) {
        log.info("Delete event — id={}", id);
        eventService.deleteEvent(id);
        return ResponseEntity.ok(Map.of("message", "Event deleted"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(IllegalArgumentException e) {
        log.warn("Event validation error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
