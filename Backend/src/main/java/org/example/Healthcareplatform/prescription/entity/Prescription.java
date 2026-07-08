package org.example.Healthcareplatform.prescription.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientUserId;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    @JsonIgnore
    private String storedFileName;

    @Column(nullable = false)
    @JsonIgnore
    private String filePath;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PrescriptionStatus status;

    @Column(length = 2000)
    private String ocrText;

    @Column(length = 500)
    private String pharmacistComments;

    private Long pharmacistId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = PrescriptionStatus.PENDING_REVIEW;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PrescriptionStatus {
        PENDING_REVIEW,
        APPROVED,
        REJECTED
    }
}
