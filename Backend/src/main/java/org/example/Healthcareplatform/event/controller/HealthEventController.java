package org.example.Healthcareplatform.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.event.dto.HealthEventRequest;
import org.example.Healthcareplatform.event.dto.HealthEventResponse;
import org.example.Healthcareplatform.event.service.HealthEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
