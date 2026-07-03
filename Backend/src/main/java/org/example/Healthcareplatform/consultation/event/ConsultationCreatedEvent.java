package org.example.Healthcareplatform.consultation.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConsultationCreatedEvent {

    private Long consultationId;
    private Long conversationId;
    private Long patientUserId;
    private String reason;
    private String priority;
    private Instant createdAt;
}
