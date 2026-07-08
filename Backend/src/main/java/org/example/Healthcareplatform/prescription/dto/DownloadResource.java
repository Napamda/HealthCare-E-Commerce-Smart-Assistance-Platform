package org.example.Healthcareplatform.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.InputStreamResource;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DownloadResource {

    private InputStreamResource resource;
    private String fileName;
    private String contentType;
    private Long fileSize;
}
