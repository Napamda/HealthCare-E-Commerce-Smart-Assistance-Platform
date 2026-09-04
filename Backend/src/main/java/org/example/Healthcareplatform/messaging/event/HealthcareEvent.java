package org.example.Healthcareplatform.messaging.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Generic event payload published to RabbitMQ.
 * All event types share this structure; the {@code type} field identifies
 * which business action fired (order.created, prescription.approved, etc.)
 * and {@code data} carries the type-specific fields.
 */
@Data
@Builder
public class HealthcareEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String type;              // e.g. "order.created"
    private Long userId;              // the user the event concerns
    private String userEmail;
    private String userName;
    private LocalDateTime timestamp;

    @Builder.Default
    private Map<String, Object> data = Map.of();
}
