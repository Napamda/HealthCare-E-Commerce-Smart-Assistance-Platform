package org.example.Healthcareplatform.consultation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationRequest {

    private Long conversationId;

    private Long patientUserId;

    private String reason;

    private String priority;
}
