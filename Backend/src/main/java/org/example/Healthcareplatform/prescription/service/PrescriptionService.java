package org.example.Healthcareplatform.prescription.service;

import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.ocr.OCRService;
import org.example.Healthcareplatform.notification.entity.Notification;
import org.example.Healthcareplatform.notification.service.NotificationService;
import org.example.Healthcareplatform.prescription.dto.DownloadResource;
import org.example.Healthcareplatform.prescription.dto.PrescriptionResponse;
import org.example.Healthcareplatform.prescription.dto.ReviewRequest;
import org.example.Healthcareplatform.prescription.dto.UploadResponse;
import org.example.Healthcareplatform.prescription.entity.Prescription;
import org.example.Healthcareplatform.prescription.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final NotificationService notificationService;
    private final OCRService ocrService;

    @Value("${prescription.storage-root:${user.home}/healthcare-uploads/prescriptions}")
    private String storageRoot;

    @Value("${prescription.max-file-size:10485760}")
    private long maxFileSize;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf"
    );

    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] GIF_MAGIC87 = new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
    private static final byte[] GIF_MAGIC89 = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46};
    private static final byte[] WEBP_MAGIC = new byte[]{0x52, 0x49, 0x46, 0x46};

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               NotificationService notificationService,
                               OCRService ocrService) {
        this.prescriptionRepository = prescriptionRepository;
        this.notificationService = notificationService;
        this.ocrService = ocrService;
    }

    public UploadResponse uploadPrescription(Long patientUserId, MultipartFile file) {
        validateFile(file);

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String extension = getFileExtension(originalFileName).toLowerCase();
        String storedFileName = generateStoredFileName(extension);

        Path dayDir = resolveStoragePath();
        ensureDirectoryExists(dayDir);

        Path targetPath = dayDir.resolve(storedFileName);
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Prescription file stored — original={}, stored={}, size={} bytes",
                    originalFileName, storedFileName, file.getSize());
        } catch (IOException e) {
            log.error("Failed to store prescription file: {}", originalFileName, e);
            throw new RuntimeException("Failed to store uploaded file", e);
        }

        Prescription prescription = Prescription.builder()
                .patientUserId(patientUserId)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .filePath(targetPath.toAbsolutePath().toString())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .status(Prescription.PrescriptionStatus.PENDING_REVIEW)
                .build();

        prescription = prescriptionRepository.save(prescription);
        log.info("Prescription record created — id={}, patientUserId={}", prescription.getId(), patientUserId);

        String ocrResult = null;
        boolean ocrAttempted = false;
        try {
            byte[] fileBytes = file.getBytes();
            ocrResult = ocrService.extractText(fileBytes, file.getContentType());
            ocrAttempted = true;
            if (ocrResult != null && !ocrResult.isBlank()) {
                prescription.setOcrText(ocrResult);
                prescriptionRepository.save(prescription);
                log.info("OCR text saved for prescription — id={}, chars={}", prescription.getId(), ocrResult.length());
            }
        } catch (Exception e) {
            log.warn("OCR failed for prescription — id={}: {}", prescription.getId(), e.getMessage());
        }

        return UploadResponse.builder()
                .prescriptionId(prescription.getId())
                .originalFileName(originalFileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .status(prescription.getStatus().name())
                .ocrText(prescription.getOcrText())
                .ocrAttempted(ocrAttempted)
                .message("Prescription uploaded successfully and is pending review.")
                .build();
    }

    public DownloadResource downloadPrescription(Long prescriptionId, Long requestingUserId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + prescriptionId));

        if (!prescription.getPatientUserId().equals(requestingUserId)) {
            log.warn("Unauthorized download attempt — prescriptionId={}, requestor={}, owner={}",
                    prescriptionId, requestingUserId, prescription.getPatientUserId());
            throw new RuntimeException("Access denied: you do not own this prescription");
        }

        Path filePath = Paths.get(prescription.getFilePath());
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new RuntimeException("Prescription file is not available");
        }

        try {
            InputStream inputStream = Files.newInputStream(filePath);
            return DownloadResource.builder()
                    .resource(new InputStreamResource(inputStream))
                    .fileName(prescription.getOriginalFileName())
                    .contentType(prescription.getFileType())
                    .fileSize(prescription.getFileSize())
                    .build();
        } catch (IOException e) {
            log.error("Failed to read prescription file — id={}", prescriptionId, e);
            throw new RuntimeException("Failed to read prescription file", e);
        }
    }

    public PrescriptionResponse getPrescription(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + id));
        return toResponse(prescription);
    }

    public List<PrescriptionResponse> getPatientPrescriptions(Long patientUserId) {
        return prescriptionRepository.findByPatientUserIdOrderByCreatedAtDesc(patientUserId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PrescriptionResponse reviewPrescription(Long prescriptionId, ReviewRequest request) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + prescriptionId));

        if (prescription.getStatus() != Prescription.PrescriptionStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Prescription has already been reviewed");
        }

        Prescription.PrescriptionStatus newStatus;
        try {
            newStatus = Prescription.PrescriptionStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status: " + request.getStatus() + ". Must be APPROVED or REJECTED");
        }

        if (newStatus != Prescription.PrescriptionStatus.APPROVED
                && newStatus != Prescription.PrescriptionStatus.REJECTED) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED");
        }

        prescription.setStatus(newStatus);
        prescription.setPharmacistComments(request.getPharmacistComments());
        prescription.setPharmacistId(request.getPharmacistId());

        prescription = prescriptionRepository.save(prescription);
        log.info("Prescription reviewed — id={}, status={}, pharmacistId={}",
                prescriptionId, newStatus, request.getPharmacistId());

        Notification.NotificationType notificationType = newStatus == Prescription.PrescriptionStatus.APPROVED
                ? Notification.NotificationType.PRESCRIPTION_APPROVED
                : Notification.NotificationType.PRESCRIPTION_REJECTED;

        String title = newStatus == Prescription.PrescriptionStatus.APPROVED
                ? "Prescription Approved"
                : "Prescription Rejected";

        String message = newStatus == Prescription.PrescriptionStatus.APPROVED
                ? "Your prescription \"" + prescription.getOriginalFileName() + "\" has been approved."
                : "Your prescription \"" + prescription.getOriginalFileName() + "\" has been rejected.";

        if (request.getPharmacistComments() != null && !request.getPharmacistComments().isBlank()) {
            message += " Pharmacist comment: " + request.getPharmacistComments();
        }

        notificationService.createNotification(
                prescription.getPatientUserId(), title, message, notificationType, prescription.getId());

        return toResponse(prescription);
    }

    public PrescriptionResponse updateOcrText(Long prescriptionId, String ocrText) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + prescriptionId));

        prescription.setOcrText(ocrText);
        prescription = prescriptionRepository.save(prescription);
        log.info("OCR text updated — prescriptionId={}, chars={}", prescriptionId,
                ocrText != null ? ocrText.length() : 0);

        return toResponse(prescription);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("File name must not be empty");
        }

        if (originalFileName.contains("..") || originalFileName.contains("/") || originalFileName.contains("\\")) {
            throw new IllegalArgumentException("File name contains invalid characters");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("File size (%d bytes) exceeds maximum allowed size of %d bytes",
                            file.getSize(), maxFileSize));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + contentType + ". Allowed: " + String.join(", ", ALLOWED_CONTENT_TYPES));
        }

        String extension = getFileExtension(originalFileName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Unsupported file extension: " + extension + ". Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        verifyMagicBytes(file, extension);
    }

    private void verifyMagicBytes(MultipartFile file, String extension) {
        try {
            byte[] header = new byte[8];
            try (InputStream is = file.getInputStream()) {
                int bytesRead = is.read(header);
                if (bytesRead < 4) {
                    throw new IllegalArgumentException("File content does not match its extension");
                }
            }

            boolean valid = switch (extension) {
                case ".jpg", ".jpeg" ->
                    header.length >= 3 && Arrays.equals(Arrays.copyOf(header, 3), JPEG_MAGIC);
                case ".png" ->
                    header.length >= 4 && Arrays.equals(Arrays.copyOf(header, 4), PNG_MAGIC);
                case ".gif" ->
                    header.length >= 6 && (Arrays.equals(Arrays.copyOf(header, 6), GIF_MAGIC87)
                            || Arrays.equals(Arrays.copyOf(header, 6), GIF_MAGIC89));
                case ".webp" ->
                    header.length >= 4 && Arrays.equals(Arrays.copyOf(header, 4), WEBP_MAGIC);
                case ".pdf" ->
                    header.length >= 4 && Arrays.equals(Arrays.copyOf(header, 4), PDF_MAGIC);
                default -> true;
            };

            if (!valid) {
                throw new IllegalArgumentException(
                        "File magic bytes do not match expected signature for extension: " + extension);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to verify file content", e);
        }
    }

    private String sanitizeFileName(String fileName) {
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9.\\-_ ]", "_").trim();
        if (sanitized.isEmpty()) {
            sanitized = "prescription_file";
        }
        return sanitized;
    }

    private String generateStoredFileName(String extension) {
        return UUID.randomUUID().toString() + extension;
    }

    private Path resolveStoragePath() {
        String resolvedRoot = storageRoot.replace("${user.home}", System.getProperty("user.home"));
        Path basePath = Paths.get(resolvedRoot).toAbsolutePath().normalize();

        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return basePath.resolve(dateDir);
    }

    private void ensureDirectoryExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.error("Failed to create storage directory: {}", dir, e);
            throw new RuntimeException("Failed to create storage directory", e);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    private PrescriptionResponse toResponse(Prescription p) {
        return PrescriptionResponse.builder()
                .id(p.getId())
                .patientUserId(p.getPatientUserId())
                .originalFileName(p.getOriginalFileName())
                .fileType(p.getFileType())
                .fileSize(p.getFileSize())
                .status(p.getStatus().name())
                .ocrText(p.getOcrText())
                .pharmacistComments(p.getPharmacistComments())
                .pharmacistId(p.getPharmacistId())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
