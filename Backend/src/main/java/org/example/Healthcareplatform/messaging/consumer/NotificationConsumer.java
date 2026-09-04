package org.example.Healthcareplatform.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.messaging.config.RabbitMQConfig;
import org.example.Healthcareplatform.messaging.event.HealthcareEvent;
import org.example.Healthcareplatform.notification.entity.Notification;
import org.example.Healthcareplatform.notification.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes domain events from the notification queue and creates
 * in-app notifications (the bell icon) for the relevant user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handle(HealthcareEvent event) {
        log.info("Notification consumer — type={}, userId={}", event.getType(), event.getUserId());
        try {
            switch (event.getType()) {
                case RabbitMQConfig.RK_ORDER_CREATED -> {
                    Long orderId = toLong(event.getData().get("orderId"));
                    String total = String.valueOf(event.getData().get("total"));
                    notificationService.createNotification(
                            event.getUserId(),
                            "Order Confirmed",
                            "Your order #" + orderId + " (" + total + ") has been created.",
                            Notification.NotificationType.ORDER_CREATED,
                            orderId);
                }
                case RabbitMQConfig.RK_PAYMENT_SUCCESS -> {
                    Long orderId = toLong(event.getData().get("orderId"));
                    String amount = String.valueOf(event.getData().get("amount"));
                    notificationService.createNotification(
                            event.getUserId(),
                            "Payment Successful",
                            "Payment of " + amount + " for order #" + orderId + " was successful.",
                            Notification.NotificationType.ORDER_CREATED,
                            orderId);
                }
                case RabbitMQConfig.RK_PRESCRIPTION_APPROVED -> {
                    Long prescriptionId = toLong(event.getData().get("prescriptionId"));
                    String status = String.valueOf(event.getData().get("status"));
                    notificationService.createNotification(
                            event.getUserId(),
                            "Prescription " + status,
                            "Your prescription #" + prescriptionId + " has been " + status.toLowerCase() + ".",
                            status.equals("APPROVED")
                                    ? Notification.NotificationType.PRESCRIPTION_APPROVED
                                    : Notification.NotificationType.PRESCRIPTION_REJECTED,
                            prescriptionId);
                }
                case RabbitMQConfig.RK_EVENT_REGISTRATION -> {
                    Long eventId = toLong(event.getData().get("eventId"));
                    String eventTitle = String.valueOf(event.getData().get("eventTitle"));
                    notificationService.createNotification(
                            event.getUserId(),
                            "Event Registration Confirmed",
                            "You are registered for \"" + eventTitle + "\".",
                            Notification.NotificationType.EVENT_REMINDER,
                            eventId);
                }
                case RabbitMQConfig.RK_CONSULTATION_SCHEDULED -> {
                    Long consultationId = toLong(event.getData().get("consultationId"));
                    String doctorName = String.valueOf(event.getData().get("doctorName"));
                    notificationService.createNotification(
                            event.getUserId(),
                            "Consultation Scheduled",
                            "Dr. " + doctorName + " has accepted your consultation request.",
                            Notification.NotificationType.CONSULTATION_ACCEPTED,
                            consultationId);
                }
                case RabbitMQConfig.RK_USER_REGISTERED -> {
                    notificationService.createNotification(
                            event.getUserId(),
                            "Welcome to HealthCare Platform",
                            "Your account has been created successfully.",
                            Notification.NotificationType.WELCOME,
                            null);
                }
                default -> log.warn("Notification consumer — unknown type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Notification consumer error — type={}, error={}", event.getType(), e.getMessage());
            throw e;
        }
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(o));
    }
}
