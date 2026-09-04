package org.example.Healthcareplatform.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorResponse {
    private Long profileId;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String businessName;
    private String businessLicense;
    private String approvalStatus;
    private String rejectionReason;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
