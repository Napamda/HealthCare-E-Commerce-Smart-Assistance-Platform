package org.example.Healthcareplatform.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.notification.entity.EmailLog;
import org.example.Healthcareplatform.notification.entity.SmsLog;
import org.example.Healthcareplatform.notification.repository.EmailLogRepository;
import org.example.Healthcareplatform.notification.repository.SmsLogRepository;
import org.example.Healthcareplatform.notification.service.EmailNotificationService;
import org.example.Healthcareplatform.notification.service.SmsSimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationLogController {

    private final EmailLogRepository emailLogRepository;
    private final SmsLogRepository smsLogRepository;
    private final EmailNotificationService emailNotificationService;
    private final SmsSimulationService smsSimulationService;

    // -------------------------------------------------------
    // Email logs
    // -------------------------------------------------------
    @GetMapping("/emails")
    public ResponseEntity<List<EmailLog>> listEmails(@RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(emailLogRepository.findByTypeOrderByCreatedAtDesc(type));
        }
        return ResponseEntity.ok(emailLogRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/emails/{id}")
    public ResponseEntity<EmailLog> getEmail(@PathVariable Long id) {
        return ResponseEntity.of(emailLogRepository.findById(id));
    }

    // -------------------------------------------------------
    // SMS logs
    // -------------------------------------------------------
    @GetMapping("/sms")
    public ResponseEntity<List<SmsLog>> listSms(@RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(smsLogRepository.findByTypeOrderByCreatedAtDesc(type));
        }
        return ResponseEntity.ok(smsLogRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/sms/{id}")
    public ResponseEntity<SmsLog> getSms(@PathVariable Long id) {
        return ResponseEntity.of(smsLogRepository.findById(id));
    }

    // -------------------------------------------------------
    // Test triggers (for demo / manual testing)
    // -------------------------------------------------------
    @PostMapping("/test/welcome")
    public ResponseEntity<Map<String, String>> testWelcomeEmail(
            @RequestParam String email,
            @RequestParam String name) {
        emailNotificationService.sendWelcomeEmail(email, name, "http://localhost:5173/products");
        smsSimulationService.simulateOrderSms("+1234567890", name, 1L, "$0.00", "http://localhost:5173/orders");
        return ResponseEntity.ok(Map.of("message", "Welcome email + SMS sent to " + email));
    }

    @PostMapping("/test/order")
    public ResponseEntity<Map<String, String>> testOrderEmail(
            @RequestParam String email,
            @RequestParam String name,
            @RequestParam Long orderId,
            @RequestParam String total) {
        emailNotificationService.sendOrderConfirmationEmail(email, name, orderId,
                List.of(Map.of("productName", "Test Product", "quantity", 1, "price", total)),
                total);
        smsSimulationService.simulateOrderSms("+1234567890", name, orderId, total,
                "http://localhost:5173/orders/" + orderId);
        return ResponseEntity.ok(Map.of("message", "Order email + SMS sent to " + email));
    }

    @PostMapping("/test/prescription")
    public ResponseEntity<Map<String, String>> testPrescriptionEmail(
            @RequestParam String email,
            @RequestParam String name,
            @RequestParam Long prescriptionId,
            @RequestParam String status,
            @RequestParam(required = false) String comments) {
        emailNotificationService.sendPrescriptionStatusEmail(email, name, prescriptionId,
                status, comments, "http://localhost:5173/prescriptions/" + prescriptionId);
        smsSimulationService.simulatePrescriptionSms("+1234567890", name, prescriptionId,
                status, comments, "http://localhost:5173/prescriptions/" + prescriptionId);
        return ResponseEntity.ok(Map.of("message", "Prescription email + SMS sent to " + email));
    }

    @PostMapping("/test/event")
    public ResponseEntity<Map<String, String>> testEventEmail(
            @RequestParam String email,
            @RequestParam String name,
            @RequestParam String eventTitle,
            @RequestParam String eventDate,
            @RequestParam(required = false) String venue) {
        emailNotificationService.sendEventReminderEmail(email, name, eventTitle,
                eventDate, venue, "http://localhost:5173/events");
        smsSimulationService.simulateEventSms("+1234567890", name, eventTitle,
                eventDate, venue, "http://localhost:5173/events");
        return ResponseEntity.ok(Map.of("message", "Event email + SMS sent to " + email));
    }
}
