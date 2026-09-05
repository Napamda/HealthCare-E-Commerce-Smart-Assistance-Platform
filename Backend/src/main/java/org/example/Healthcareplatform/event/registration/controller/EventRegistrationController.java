package org.example.Healthcareplatform.event.registration.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.event.registration.dto.EventRegistrationRequest;
import org.example.Healthcareplatform.event.registration.dto.EventRegistrationResponse;
import org.example.Healthcareplatform.event.registration.dto.EventRegistrationStatusResponse;
import org.example.Healthcareplatform.event.registration.service.EventRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/event-registrations")
@RequiredArgsConstructor
@Slf4j
public class EventRegistrationController {

    private final EventRegistrationService registrationService;

    @PostMapping
    public ResponseEntity<EventRegistrationResponse> register(@RequestBody EventRegistrationRequest request) {
        log.info("Register for event — eventId={}", request.getEventId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.register(request.getEventId()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<EventRegistrationResponse>> myRegistrations() {
        log.info("List my event registrations");
        return ResponseEntity.ok(registrationService.myRegistrations());
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<EventRegistrationStatusResponse> eventStatus(@PathVariable Long eventId) {
        log.info("Event registration status — eventId={}", eventId);
        return ResponseEntity.ok(registrationService.status(eventId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> cancel(@PathVariable Long id) {
        log.info("Cancel event registration — id={}", id);
        registrationService.cancel(id);
        return ResponseEntity.ok(Map.of("message", "Registration cancelled"));
    }

}
