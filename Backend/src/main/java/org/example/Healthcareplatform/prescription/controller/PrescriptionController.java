package org.example.Healthcareplatform.prescription.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.prescription.dto.DownloadResource;
import org.example.Healthcareplatform.prescription.dto.PrescriptionResponse;
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
