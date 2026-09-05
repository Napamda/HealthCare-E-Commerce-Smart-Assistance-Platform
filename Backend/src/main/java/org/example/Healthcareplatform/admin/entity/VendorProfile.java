package org.example.Healthcareplatform.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Vendor application / approval tracking. Created automatically
 * when a user registers with the VENDOR role (Task 3.2).
 */
@Entity
@Table(name = "vendor_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String businessName;

    @Column(length = 200)
    private String businessLicense;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(length = 500)
    private String rejectionReason;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
