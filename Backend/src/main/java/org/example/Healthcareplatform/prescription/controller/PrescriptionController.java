package org.example.Healthcareplatform.prescription.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.prescription.dto.DownloadResource;
import org.example.Healthcareplatform.prescription.dto.PrescriptionResponse;
import org.example.Healthcareplatform.prescription.dto.ReviewRequest;
import org.example.Healthcareplatform.prescription.dto.UploadResponse;
import org.example.Healthcareplatform.prescription.service.PrescriptionService;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.entity.UserRole;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@Slf4j
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadPrescription(
            @RequestParam("file") MultipartFile file,
            org.springframework.security.core.Authentication auth) {
        // Patient id comes from the authenticated session, never from the request.
        Long patientUserId = Long.parseLong(auth.getName());
        log.info("Prescription upload request — patientUserId={}, fileName={}, size={}",
                patientUserId, file.getOriginalFilename(), file.getSize());
        UploadResponse response = prescriptionService.uploadPrescription(patientUserId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponse> getPrescription(
            @PathVariable Long id,
            org.springframework.security.core.Authentication auth) {
        Long callerId = Long.parseLong(auth.getName());
        PrescriptionResponse prescription = prescriptionService.getPrescription(id);
        User caller = userRepository.findById(callerId).orElse(null);
        boolean isStaff = caller != null && (
                caller.getRole() == UserRole.PHARMACIST
                        || caller.getRole() == UserRole.ADMIN
                        || caller.getRole() == UserRole.DOCTOR);
        if (!isStaff && !prescription.getPatientUserId().equals(callerId)) {
            throw new AccessDeniedException("You cannot view this prescription");
        }
        log.info("Get prescription — id={}, caller={}, isStaff={}", id, callerId, isStaff);
        return ResponseEntity.ok(prescription);
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<PrescriptionResponse> reviewPrescription(
            @PathVariable Long id,
            @RequestBody ReviewRequest request) {
        log.info("Review prescription — id={}, status={}, pharmacistId={}, items={}",
                id, request.getStatus(), request.getPharmacistId(),
                request.getItems() == null ? 0 : request.getItems().size());
        PrescriptionResponse response = prescriptionService.reviewPrescription(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Patient adds the pharmacist-selected medications to their own cart.
     * The patient id is taken from the authenticated session — the pharmacist
     * has no way to order on the patient's behalf.
     */
    @PostMapping("/{id}/order")
    public ResponseEntity<?> orderPrescription(
            @PathVariable Long id,
            org.springframework.security.core.Authentication auth) {
        Long patientUserId = Long.parseLong(auth.getName());
        log.info("Order prescription medications — id={}, patientUserId={}", id, patientUserId);
        try {
            PrescriptionResponse response = prescriptionService.orderPrescription(id, patientUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/ocr")
    public ResponseEntity<PrescriptionResponse> updateOcrText(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String ocrText = body.get("ocrText");
        log.info("Update OCR text — id={}, chars={}", id, ocrText != null ? ocrText.length() : 0);
        PrescriptionResponse response = prescriptionService.updateOcrText(id, ocrText);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadPrescription(
            @PathVariable Long id,
            org.springframework.security.core.Authentication auth) {
        Long callerId = Long.parseLong(auth.getName());
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
        boolean isStaff = caller.getRole() == UserRole.PHARMACIST || caller.getRole() == UserRole.ADMIN
                || caller.getRole() == UserRole.DOCTOR;

        PrescriptionResponse prescription = prescriptionService.getPrescription(id);
        if (!isStaff && !prescription.getPatientUserId().equals(callerId)) {
            throw new AccessDeniedException("You cannot download this prescription");
        }
        log.info("Download prescription — id={}, userId={}, isStaff={}", id, callerId, isStaff);
        DownloadResource download = prescriptionService.downloadPrescription(id, callerId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .contentLength(download.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + download.getFileName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(download.getResource());
    }

    /**
     * Patient listing: id is always taken from the authenticated user so a logged
     * in patient sees exactly their own prescriptions regardless of request args.
     */
    @GetMapping("/mine")
    public ResponseEntity<List<PrescriptionResponse>> getMyPrescriptions(
            org.springframework.security.core.Authentication auth) {
        Long patientUserId = Long.parseLong(auth.getName());
        log.info("List prescriptions for patient — patientUserId={}", patientUserId);
        return ResponseEntity.ok(prescriptionService.getPatientPrescriptions(patientUserId));
    }

    @Deprecated
    @GetMapping("/patient/{patientUserId}")
    public ResponseEntity<List<PrescriptionResponse>> getPatientPrescriptions(
            @PathVariable Long patientUserId,
            org.springframework.security.core.Authentication auth) {
        Long callerId = Long.parseLong(auth.getName());
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
        boolean isStaff = caller.getRole() == UserRole.PHARMACIST || caller.getRole() == UserRole.ADMIN
                || caller.getRole() == UserRole.DOCTOR;
        if (!isStaff && !patientUserId.equals(callerId)) {
            // Override — never let a patient fetch someone else's list.
            patientUserId = callerId;
        }
        log.info("List prescriptions for patient — patientUserId={}, caller={}", patientUserId, callerId);
        return ResponseEntity.ok(prescriptionService.getPatientPrescriptions(patientUserId));
    }

    @GetMapping("/pharmacist/pending")
    public ResponseEntity<List<PrescriptionResponse>> getPendingPrescriptions() {
        log.info("List all pending prescriptions");
        return ResponseEntity.ok(prescriptionService.getPendingPrescriptions());
    }

    @GetMapping("/pharmacist/search")
    public ResponseEntity<List<PrescriptionResponse>> searchPrescriptions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("Search prescriptions — status={}, search={}, startDate={}, endDate={}",
                status, search, startDate, endDate);
        return ResponseEntity.ok(
                prescriptionService.searchPrescriptions(status, search, startDate, endDate));
    }

}
