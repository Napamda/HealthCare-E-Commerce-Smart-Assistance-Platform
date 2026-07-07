package org.example.Healthcareplatform.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
}
