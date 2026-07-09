package org.example.Healthcareplatform.consultation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationResponse {

    private Long id;

    private Long conversationId;

    private Long patientUserId;

    private String patientName;

    private Long doctorUserId;

    private String doctorName;

    private String status;

    private String priority;

    private String reason;

    private String chatContextSummary;

    private String doctorNotes;

    private int messageCount;

    private Instant createdAt;

    private Instant updatedAt;
}
