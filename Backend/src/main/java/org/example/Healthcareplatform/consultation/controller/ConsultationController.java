package org.example.Healthcareplatform.consultation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.example.Healthcareplatform.consultation.dto.ConsultationResponse;
import org.example.Healthcareplatform.consultation.dto.EscalationRequest;
import org.example.Healthcareplatform.consultation.service.ConsultationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;
    private final SecurityContextUtil securityContextUtil;

    @PostMapping("/escalate")
    public ResponseEntity<ConsultationResponse> escalate(@RequestBody EscalationRequest request) {
        Long patientUserId = securityContextUtil.getCurrentUserId();
        log.info("POST /api/consultations/escalate — conversationId={}, patientUserId={}, priority={}",
                request.getConversationId(), patientUserId, request.getPriority());

        // Doctors review escalated consultations; a doctor must not escalate
        // their own AI chat to "a doctor".
        if ("DOCTOR".equals(securityContextUtil.getCurrentUserRole())) {
            log.warn("POST /api/consultations/escalate denied — doctors cannot escalate their own chat (userId={})",
                    patientUserId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (request.getConversationId() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ConsultationResponse response = consultationService.escalateFromChat(request, patientUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsultationResponse> getConsultation(@PathVariable Long id) {
        log.info("GET /api/consultations/{}", id);

        try {
            ConsultationResponse response = consultationService.getConsultation(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/patient/{patientUserId}")
    public ResponseEntity<List<ConsultationResponse>> getPatientConsultations(
            @PathVariable Long patientUserId) {
        Long currentUserId = securityContextUtil.getCurrentUserId();
        log.info("GET /api/consultations/patient/{} — caller userId={}", patientUserId, currentUserId);

        if (!currentUserId.equals(patientUserId)
                && !securityContextUtil.getCurrentUserRole().equals("ADMIN")
                && !securityContextUtil.getCurrentUserRole().equals("DOCTOR")) {
            return ResponseEntity.status(403).build();
        }

        List<ConsultationResponse> consultations = consultationService.getPatientConsultations(patientUserId);
        return ResponseEntity.ok(consultations);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ConsultationResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String rejectionReason,
            @RequestParam(required = false) String scheduledAt) {
        // Only real doctors act on consultations. Admins can view queues via
        // the preview routes but must not claim or move patients' requests.
        String role = securityContextUtil.getCurrentUserRole();
        if (!"DOCTOR".equals(role)) {
            log.warn("PATCH /api/consultations/{}/status denied for role={}", id, role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long doctorUserId = securityContextUtil.getCurrentUserId();
        log.info("PATCH /api/consultations/{}/status — status={}, doctorUserId={}, rejectionReason={}, scheduledAt={}",
                id, status, doctorUserId, rejectionReason, scheduledAt);

        Instant scheduledInstant = null;
        if (scheduledAt != null && !scheduledAt.isBlank()) {
            scheduledInstant = Instant.parse(scheduledAt); // 400 via global handler on malformed input
        }

        ConsultationResponse response = consultationService.updateStatus(
                id, status, doctorUserId, rejectionReason, scheduledInstant);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctor/queue")
    public ResponseEntity<List<ConsultationResponse>> getDoctorQueue() {
        Long doctorUserId = securityContextUtil.getCurrentUserId();
        String role = securityContextUtil.getCurrentUserRole();
        log.info("GET /api/consultations/doctor/queue — doctorUserId={}, role={}", doctorUserId, role);

        if (!role.equals("DOCTOR") && !role.equals("ADMIN")) {
            return ResponseEntity.status(403).build();
        }

        List<ConsultationResponse> queue = consultationService.getDoctorQueue(doctorUserId);
        return ResponseEntity.ok(queue);
    }

    @GetMapping("/doctor/upcoming")
    public ResponseEntity<List<ConsultationResponse>> getDoctorUpcoming() {
        Long doctorUserId = securityContextUtil.getCurrentUserId();
        String role = securityContextUtil.getCurrentUserRole();
        log.info("GET /api/consultations/doctor/upcoming — doctorUserId={}, role={}", doctorUserId, role);

        if (!role.equals("DOCTOR") && !role.equals("ADMIN")) {
            return ResponseEntity.status(403).build();
        }

        List<ConsultationResponse> upcoming = consultationService.getDoctorUpcoming(doctorUserId);
        return ResponseEntity.ok(upcoming);
    }

    @PatchMapping("/{id}/priority")
    public ResponseEntity<ConsultationResponse> updatePriority(
            @PathVariable Long id,
            @RequestParam String priority) {
        log.info("PATCH /api/consultations/{}/priority — priority={}", id, priority);

        try {
            ConsultationResponse response = consultationService.updatePriority(id, priority);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(null);
        }
    }

}
