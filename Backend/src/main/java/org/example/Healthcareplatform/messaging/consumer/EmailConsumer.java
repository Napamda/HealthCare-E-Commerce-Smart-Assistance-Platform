package org.example.Healthcareplatform.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.messaging.config.RabbitMQConfig;
import org.example.Healthcareplatform.messaging.event.HealthcareEvent;
import org.example.Healthcareplatform.notification.service.EmailNotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Consumes domain events from the email queue and dispatches
 * Thymeleaf-templated emails via EmailNotificationService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {

    private final EmailNotificationService emailNotificationService;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void handle(HealthcareEvent event) {
        log.info("Email consumer — type={}, userId={}", event.getType(), event.getUserId());
        try {
            String email = event.getUserEmail();
            String name = event.getUserName() != null ? event.getUserName() : "User";

            switch (event.getType()) {
                case RabbitMQConfig.RK_ORDER_CREATED -> {
                    Long orderId = toLong(event.getData().get("orderId"));
                    String total = String.valueOf(event.getData().get("total"));
                    emailNotificationService.sendOrderConfirmationEmail(
                            email, name, orderId,
                            List.of(Map.of("productName", "Items in order", "quantity", toInt(event.getData().get("itemCount")), "price", total)),
                            total);
                }
                case RabbitMQConfig.RK_PAYMENT_SUCCESS -> {
                    Long orderId = toLong(event.getData().get("orderId"));
                    String amount = String.valueOf(event.getData().get("amount"));
                    emailNotificationService.sendOrderConfirmationEmail(
                            email, name, orderId,
                            List.of(Map.of("productName", "Payment", "quantity", 1, "price", amount)),
                            amount);
                }
                case RabbitMQConfig.RK_PRESCRIPTION_APPROVED -> {
                    Long prescriptionId = toLong(event.getData().get("prescriptionId"));
                    String status = String.valueOf(event.getData().get("status"));
                    String comments = String.valueOf(event.getData().get("comments"));
                    emailNotificationService.sendPrescriptionStatusEmail(
                            email, name, prescriptionId, status, comments,
                            "http://localhost:5173/prescriptions/" + prescriptionId);
                }
                case RabbitMQConfig.RK_EVENT_REGISTRATION -> {
                    String eventTitle = String.valueOf(event.getData().get("eventTitle"));
                    emailNotificationService.sendEventReminderEmail(
                            email, name, eventTitle, "See event page", "",
                            "http://localhost:5173/events");
                }
                case RabbitMQConfig.RK_CONSULTATION_SCHEDULED -> {
                    String doctorName = String.valueOf(event.getData().get("doctorName"));
                    emailNotificationService.sendPrescriptionStatusEmail(
                            email, name, 0L, "CONSULTATION",
                            "Dr. " + doctorName + " will be consulting with you.",
                            "http://localhost:5173/chat");
                }
                case RabbitMQConfig.RK_USER_REGISTERED -> {
                    emailNotificationService.sendWelcomeEmail(
                            email, name, "http://localhost:5173/products");
                }
                default -> log.warn("Email consumer — unknown type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Email consumer error — type={}, error={}", event.getType(), e.getMessage());
            throw e;
        }
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(o));
    }

    private int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Integer i) return i;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(o));
    }
}
