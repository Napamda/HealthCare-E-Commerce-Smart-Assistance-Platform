package org.example.Healthcareplatform.prescription.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.prescription.dto.DownloadResource;
import org.example.Healthcareplatform.prescription.dto.PrescriptionResponse;
import org.example.Healthcareplatform.prescription.dto.ReviewRequest;
import org.example.Healthcareplatform.prescription.dto.UploadResponse;
import org.example.Healthcareplatform.prescription.service.PrescriptionService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadPrescription(
            @RequestParam("file") MultipartFile file,
            @RequestParam("patientUserId") Long patientUserId) {
        log.info("Prescription upload request — patientUserId={}, fileName={}, size={}",
                patientUserId, file.getOriginalFilename(), file.getSize());
        UploadResponse response = prescriptionService.uploadPrescription(patientUserId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponse> getPrescription(@PathVariable Long id) {
        log.info("Get prescription — id={}", id);
        return ResponseEntity.ok(prescriptionService.getPrescription(id));
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<PrescriptionResponse> reviewPrescription(
            @PathVariable Long id,
            @RequestBody ReviewRequest request) {
        log.info("Review prescription — id={}, status={}, pharmacistId={}",
                id, request.getStatus(), request.getPharmacistId());
        PrescriptionResponse response = prescriptionService.reviewPrescription(id, request);
        return ResponseEntity.ok(response);
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
            @RequestParam("userId") Long userId) {
        log.info("Download prescription — id={}, userId={}", id, userId);
        DownloadResource download = prescriptionService.downloadPrescription(id, userId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .contentLength(download.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + download.getFileName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(download.getResource());
    }

    @GetMapping("/patient/{patientUserId}")
    public ResponseEntity<List<PrescriptionResponse>> getPatientPrescriptions(
            @PathVariable Long patientUserId) {
        log.info("List prescriptions for patient — patientUserId={}", patientUserId);
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(IllegalArgumentException e) {
        log.warn("Prescription validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeError(RuntimeException e) {
        log.error("Prescription error: {}", e.getMessage(), e);
        if (e.getMessage() != null && e.getMessage().contains("Access denied")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
        if (e.getMessage() != null && e.getMessage().contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
    }
}
