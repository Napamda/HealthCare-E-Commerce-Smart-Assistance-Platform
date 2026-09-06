package org.example.Healthcareplatform.prescription.service;

import org.example.Healthcareplatform.ai.ocr.OCRService;
import org.example.Healthcareplatform.cart.service.CartService;
import org.example.Healthcareplatform.notification.entity.Notification;
import org.example.Healthcareplatform.notification.service.EmailNotificationService;
import org.example.Healthcareplatform.notification.service.NotificationService;
import org.example.Healthcareplatform.notification.service.SmsSimulationService;
import org.example.Healthcareplatform.messaging.publisher.HealthcareEventPublisher;
import org.example.Healthcareplatform.prescription.dto.PrescriptionResponse;
import org.example.Healthcareplatform.prescription.dto.ReviewRequest;
import org.example.Healthcareplatform.prescription.dto.UploadResponse;
import org.example.Healthcareplatform.prescription.entity.Prescription;
import org.example.Healthcareplatform.prescription.repository.PrescriptionItemRepository;
import org.example.Healthcareplatform.prescription.repository.PrescriptionRepository;
import org.example.Healthcareplatform.product.repository.ProductRepository;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PrescriptionItemRepository prescriptionItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartService cartService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private OCRService ocrService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private SmsSimulationService smsSimulationService;

    @Mock
    private HealthcareEventPublisher eventPublisher;

    @InjectMocks
    private PrescriptionService prescriptionService;

    private Prescription pendingPrescription;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                prescriptionService,
                "maxFileSize",
                10_485_760L
        );
        ReflectionTestUtils.setField(
                prescriptionService,
                "storageRoot",
                "target/test-uploads/prescriptions"
        );

        // New dependencies: responses carry an (empty) medication list.
        lenient().when(prescriptionItemRepository.findByPrescriptionIdOrderByIdAsc(anyLong()))
                .thenReturn(List.of());

        // Email/SMS notifications look up the patient — return empty so
        // the ifPresent calls are no-ops.
        lenient().when(userRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        pendingPrescription = Prescription.builder()
                .id(10L)
                .patientUserId(1L)
                .originalFileName("prescription.pdf")
                .storedFileName("stored.pdf")
                .filePath("/tmp/prescription.pdf")
                .fileType("application/pdf")
                .fileSize(100L)
                .status(Prescription.PrescriptionStatus.PENDING_REVIEW)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldApprovePendingPrescriptionAndNotifyPatient() {
        ReviewRequest request = ReviewRequest.builder()
                .status("APPROVED")
                .pharmacistId(5L)
                .pharmacistComments("Valid prescription")
                .build();

        when(prescriptionRepository.findById(10L))
                .thenReturn(Optional.of(pendingPrescription));

        when(prescriptionRepository.save(any(Prescription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrescriptionResponse result =
                prescriptionService.reviewPrescription(10L, request);

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(pendingPrescription.getPharmacistId()).isEqualTo(5L);
        assertThat(pendingPrescription.getPharmacistComments())
                .isEqualTo("Valid prescription");

        verify(notificationService).createNotification(
                eq(1L),
                eq("Prescription Approved"),
                contains("Valid prescription"),
                eq(Notification.NotificationType.PRESCRIPTION_APPROVED),
                eq(10L)
        );
    }

    @Test
    void shouldRejectPendingPrescriptionAndNotifyPatient() {
        ReviewRequest request = ReviewRequest.builder()
                .status("REJECTED")
                .pharmacistId(5L)
                .pharmacistComments("Image is unclear")
                .build();

        when(prescriptionRepository.findById(10L))
                .thenReturn(Optional.of(pendingPrescription));

        when(prescriptionRepository.save(any(Prescription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrescriptionResponse result =
                prescriptionService.reviewPrescription(10L, request);

        assertThat(result.getStatus()).isEqualTo("REJECTED");

        verify(notificationService).createNotification(
                eq(1L),
                eq("Prescription Rejected"),
                contains("Image is unclear"),
                eq(Notification.NotificationType.PRESCRIPTION_REJECTED),
                eq(10L)
        );
    }

    @Test
    void shouldRejectReviewWhenPrescriptionAlreadyReviewed() {
        pendingPrescription.setStatus(
                Prescription.PrescriptionStatus.APPROVED
        );

        when(prescriptionRepository.findById(10L))
                .thenReturn(Optional.of(pendingPrescription));

        ReviewRequest request = ReviewRequest.builder()
                .status("REJECTED")
                .pharmacistId(5L)
                .build();

        assertThatThrownBy(() ->
                prescriptionService.reviewPrescription(10L, request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prescription has already been reviewed");

        verify(prescriptionRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldRejectInvalidReviewStatus() {
        when(prescriptionRepository.findById(10L))
                .thenReturn(Optional.of(pendingPrescription));

        ReviewRequest request = ReviewRequest.builder()
                .status("INVALID")
                .pharmacistId(5L)
                .build();

        assertThatThrownBy(() ->
                prescriptionService.reviewPrescription(10L, request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status");

        verify(prescriptionRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldRejectPendingReviewAsNewReviewStatus() {
        when(prescriptionRepository.findById(10L))
                .thenReturn(Optional.of(pendingPrescription));

        ReviewRequest request = ReviewRequest.builder()
                .status("PENDING_REVIEW")
                .pharmacistId(5L)
                .build();

        assertThatThrownBy(() ->
                prescriptionService.reviewPrescription(10L, request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Status must be APPROVED or REJECTED");
    }

    @Test
    void shouldReturnPendingPrescriptions() {
        when(prescriptionRepository.findByStatusOrderByCreatedAtDesc(
                Prescription.PrescriptionStatus.PENDING_REVIEW
        )).thenReturn(List.of(pendingPrescription));

        List<PrescriptionResponse> result =
                prescriptionService.getPendingPrescriptions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(0).getStatus())
                .isEqualTo("PENDING_REVIEW");
    }

    @Test
    void shouldSearchByFileName() {
        Prescription other = prescription(
                11L,
                2L,
                "blood-test.pdf",
                "Other OCR",
                Prescription.PrescriptionStatus.APPROVED
        );

        when(prescriptionRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(pendingPrescription, other));

        List<PrescriptionResponse> result =
                prescriptionService.searchPrescriptions(
                        null,
                        "blood",
                        null,
                        null
                );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(11L);
    }

    @Test
    void shouldSearchByOcrText() {
        pendingPrescription.setOcrText("Take amoxicillin twice daily");

        when(prescriptionRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(pendingPrescription));

        List<PrescriptionResponse> result =
                prescriptionService.searchPrescriptions(
                        null,
                        "amoxicillin",
                        null,
                        null
                );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
    }

    @Test
    void shouldFilterByStatus() {
        Prescription approved = prescription(
                11L,
                2L,
                "approved.pdf",
                null,
                Prescription.PrescriptionStatus.APPROVED
        );

        when(prescriptionRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(pendingPrescription, approved));

        List<PrescriptionResponse> result =
                prescriptionService.searchPrescriptions(
                        "APPROVED",
                        null,
                        null,
                        null
                );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(11L);
    }

    @Test
    void shouldReturnAllRecordsWhenDateFilterIsInvalid() {
        when(prescriptionRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(pendingPrescription));

        List<PrescriptionResponse> result =
                prescriptionService.searchPrescriptions(
                        null,
                        null,
                        "not-a-date",
                        "also-not-a-date"
                );

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldUpdateOcrText() {
        when(prescriptionRepository.findById(10L))
                .thenReturn(Optional.of(pendingPrescription));

        when(prescriptionRepository.save(any(Prescription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrescriptionResponse result =
                prescriptionService.updateOcrText(
                        10L,
                        "Corrected OCR result"
                );

        assertThat(result.getOcrText())
                .isEqualTo("Corrected OCR result");
    }

    @Test
    void shouldRejectEmptyFileBeforeStorage() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() ->
                prescriptionService.uploadPrescription(1L, file)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File must not be empty");

        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("file.exe");
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("application/octet-stream");

        assertThatThrownBy(() ->
                prescriptionService.uploadPrescription(1L, file)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");

        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    void shouldRejectPathTraversalFileName() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("../secret.pdf");

        assertThatThrownBy(() ->
                prescriptionService.uploadPrescription(1L, file)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File name contains invalid characters");
    }

    @Test
    void shouldRejectFileExceedingMaximumSize() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("large.pdf");
        when(file.getSize()).thenReturn(20_000_000L);

        assertThatThrownBy(() ->
                prescriptionService.uploadPrescription(1L, file)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size");
    }

    @Test
    void shouldRejectMagicBytesThatDoNotMatchExtension() throws IOException {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("fake.pdf");
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getInputStream())
                .thenReturn(new ByteArrayInputStream(
                        new byte[]{1, 2, 3, 4, 5, 6, 7, 8}
                ));

        assertThatThrownBy(() ->
                prescriptionService.uploadPrescription(1L, file)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    void shouldAcceptValidPdfUpload() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        byte[] pdfHeader = new byte[]{'%', 'P', 'D', 'F', '-', '1', '.', '4'};

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("prescription.pdf");
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(pdfHeader));
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenAnswer(invocation -> {
                    Prescription saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });
        when(ocrService.extractText(any(), any())).thenReturn("Take medicine daily");

        UploadResponse result =
                prescriptionService.uploadPrescription(1L, file);

        assertThat(result.getPrescriptionId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("PENDING_REVIEW");
        verify(prescriptionRepository, times(2)).save(any(Prescription.class));
    }

    @Test
    void shouldReturnEmptySearchResults() {
        when(prescriptionRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(pendingPrescription));

        List<PrescriptionResponse> result =
                prescriptionService.searchPrescriptions(
                        null,
                        "nonexistent-keyword",
                        null,
                        null
                );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowWhenPrescriptionNotFoundForReview() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());

        ReviewRequest request = ReviewRequest.builder()
                .status("APPROVED")
                .pharmacistId(5L)
                .build();

        assertThatThrownBy(() ->
                prescriptionService.reviewPrescription(99L, request)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPrescriptionNotFoundForOcrUpdate() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                prescriptionService.updateOcrText(99L, "Corrected")
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldReturnPrescriptionById() {
        when(prescriptionRepository.findById(10L))
                .thenReturn(Optional.of(pendingPrescription));

        PrescriptionResponse result =
                prescriptionService.getPrescription(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void shouldThrowWhenPrescriptionNotFoundById() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prescriptionService.getPrescription(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldSearchByPatientId() {
        when(prescriptionRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(
                        pendingPrescription,
                        prescription(11L, 2L, "other.pdf", null,
                                Prescription.PrescriptionStatus.APPROVED)
                ));

        List<PrescriptionResponse> result =
                prescriptionService.searchPrescriptions(
                        null, "1", null, null
                );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
    }

    @Test
    void shouldFilterByValidDateRange() {
        when(prescriptionRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(pendingPrescription));

        List<PrescriptionResponse> result =
                prescriptionService.searchPrescriptions(
                        null,
                        null,
                        "2026-01-01",
                        "2026-12-31"
                );

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldRejectRejectedPrescriptionBeingApprovedAgain() {
        pendingPrescription.setStatus(Prescription.PrescriptionStatus.REJECTED);

        when(prescriptionRepository.findById(10L))
                .thenReturn(Optional.of(pendingPrescription));

        ReviewRequest request = ReviewRequest.builder()
                .status("APPROVED")
                .pharmacistId(5L)
                .build();

        assertThatThrownBy(() ->
                prescriptionService.reviewPrescription(10L, request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prescription has already been reviewed");
    }

    private Prescription prescription(
            Long id,
            Long patientId,
            String fileName,
            String ocrText,
            Prescription.PrescriptionStatus status
    ) {
        return Prescription.builder()
                .id(id)
                .patientUserId(patientId)
                .originalFileName(fileName)
                .storedFileName("stored-" + id + ".pdf")
                .filePath("/tmp/file-" + id + ".pdf")
                .fileType("application/pdf")
                .fileSize(100L)
                .ocrText(ocrText)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
