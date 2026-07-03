package org.example.Healthcareplatform.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadResponse {

    private Long prescriptionId;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String status;
    private String message;
}
