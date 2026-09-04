package org.example.Healthcareplatform.messaging.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.messaging.config.RabbitMQConfig;
import org.example.Healthcareplatform.messaging.event.HealthcareEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Central event publisher — services call these methods to publish
 * domain events to the RabbitMQ topic exchange.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HealthcareEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    // -------------------------------------------------------
    // Order events
    // -------------------------------------------------------
    public void publishOrderCreated(Long orderId, Long userId, String userEmail,
                                    String userName, String total, int itemCount) {
        HealthcareEvent event = HealthcareEvent.builder()
                .type(RabbitMQConfig.RK_ORDER_CREATED)
                .userId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "orderId", orderId,
                        "total", total,
                        "itemCount", itemCount
                ))
                .build();
        send(event);
    }

    public void publishPaymentSuccess(Long orderId, Long userId, String userEmail,
                                      String userName, String amount) {
        HealthcareEvent event = HealthcareEvent.builder()
                .type(RabbitMQConfig.RK_PAYMENT_SUCCESS)
                .userId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "orderId", orderId,
                        "amount", amount
                ))
                .build();
        send(event);
    }

    // -------------------------------------------------------
    // Prescription events
    // -------------------------------------------------------
    public void publishPrescriptionApproved(Long prescriptionId, Long patientUserId,
                                             String patientEmail, String patientName,
                                             String status, String comments) {
        HealthcareEvent event = HealthcareEvent.builder()
                .type(RabbitMQConfig.RK_PRESCRIPTION_APPROVED)
                .userId(patientUserId)
                .userEmail(patientEmail)
                .userName(patientName)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "prescriptionId", prescriptionId,
                        "status", status,
                        "comments", comments != null ? comments : ""
                ))
                .build();
        send(event);
    }

    // -------------------------------------------------------
    // Event registration events
    // -------------------------------------------------------
    public void publishEventRegistration(Long eventId, Long userId, String userEmail,
                                         String userName, String eventTitle, String volunteerRole) {
        HealthcareEvent event = HealthcareEvent.builder()
                .type(RabbitMQConfig.RK_EVENT_REGISTRATION)
                .userId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "eventId", eventId,
                        "eventTitle", eventTitle != null ? eventTitle : "",
                        "volunteerRole", volunteerRole != null ? volunteerRole : ""
                ))
                .build();
        send(event);
    }

    // -------------------------------------------------------
    // Consultation events
    // -------------------------------------------------------
    public void publishConsultationScheduled(Long consultationId, Long patientUserId,
                                             String patientEmail, String patientName,
                                             Long doctorUserId, String doctorName) {
        HealthcareEvent event = HealthcareEvent.builder()
                .type(RabbitMQConfig.RK_CONSULTATION_SCHEDULED)
                .userId(patientUserId)
                .userEmail(patientEmail)
                .userName(patientName)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "consultationId", consultationId,
                        "doctorUserId", doctorUserId,
                        "doctorName", doctorName != null ? doctorName : ""
                ))
                .build();
        send(event);
    }

    // -------------------------------------------------------
    // User registration events
    // -------------------------------------------------------
    public void publishUserRegistered(Long userId, String email, String name) {
        HealthcareEvent event = HealthcareEvent.builder()
                .type(RabbitMQConfig.RK_USER_REGISTERED)
                .userId(userId)
                .userEmail(email)
                .userName(name)
                .timestamp(LocalDateTime.now())
                .data(Map.of())
                .build();
        send(event);
    }

    // -------------------------------------------------------
    // Internal send
    // -------------------------------------------------------
    private void send(HealthcareEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    event.getType(),
                    event);
            log.info("Event published — type={}, eventId={}, userId={}",
                    event.getType(), event.getEventId(), event.getUserId());
        } catch (Exception e) {
            log.warn("Failed to publish event — type={}, error={}",
                    event.getType(), e.getMessage());
        }
    }
}
