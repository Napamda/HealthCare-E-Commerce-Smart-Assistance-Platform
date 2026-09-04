package org.example.Healthcareplatform.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    /** Target status: ACTIVE or SUSPENDED. */
    @NotNull(message = "status is required (ACTIVE or SUSPENDED)")
    private String status;

    /** Optional reason, stored on the user and in the activity log when suspending. */
    @Size(max = 255, message = "Reason must be at most 255 characters")
    private String reason;
}
