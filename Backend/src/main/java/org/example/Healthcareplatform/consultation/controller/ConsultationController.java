package org.example.Healthcareplatform.consultation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.example.Healthcareplatform.consultation.dto.ConsultationResponse;
import org.example.Healthcareplatform.consultation.dto.EscalationRequest;
import org.example.Healthcareplatform.consultation.service.ConsultationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
            @RequestParam String status) {
        Long doctorUserId = securityContextUtil.getCurrentUserId();
        log.info("PATCH /api/consultations/{}/status — status={}, doctorUserId={}", id, status, doctorUserId);

        try {
            ConsultationResponse response = consultationService.updateStatus(id, status, doctorUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(null);
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
    }
}
