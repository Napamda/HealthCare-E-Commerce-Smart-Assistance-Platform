package org.example.Healthcareplatform.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponse {

    private Long id;
    private Long patientUserId;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String status;
    private String ocrText;
    private String pharmacistComments;
    private Long pharmacistId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Medications the pharmacist selected for this prescription. Populated for
     * approved prescriptions so the patient can order them directly.
     */
    private List<PrescriptionItemResponse> items;
}
