package org.example.Healthcareplatform.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.messaging.config.RabbitMQConfig;
import org.example.Healthcareplatform.messaging.event.HealthcareEvent;
import org.example.Healthcareplatform.notification.service.SmsSimulationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes domain events from the SMS queue and generates
 * simulated SMS logs via SmsSimulationService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmsConsumer {

    private final SmsSimulationService smsSimulationService;

    @RabbitListener(queues = RabbitMQConfig.SMS_QUEUE)
    public void handle(HealthcareEvent event) {
        log.info("SMS consumer — type={}, userId={}", event.getType(), event.getUserId());
        try {
            String name = event.getUserName() != null ? event.getUserName() : "User";
            String phone = "+0000000000";

            switch (event.getType()) {
                case RabbitMQConfig.RK_ORDER_CREATED -> {
                    Long orderId = toLong(event.getData().get("orderId"));
                    String total = String.valueOf(event.getData().get("total"));
                    smsSimulationService.simulateOrderSms(
                            phone, name, orderId, total,
                            "http://localhost:5173/orders/" + orderId);
                }
                case RabbitMQConfig.RK_PAYMENT_SUCCESS -> {
                    Long orderId = toLong(event.getData().get("orderId"));
                    String amount = String.valueOf(event.getData().get("amount"));
                    smsSimulationService.simulateOrderSms(
                            phone, name, orderId, amount,
                            "http://localhost:5173/orders/" + orderId);
                }
                case RabbitMQConfig.RK_PRESCRIPTION_APPROVED -> {
                    Long prescriptionId = toLong(event.getData().get("prescriptionId"));
                    String status = String.valueOf(event.getData().get("status"));
                    String comments = String.valueOf(event.getData().get("comments"));
                    smsSimulationService.simulatePrescriptionSms(
                            phone, name, prescriptionId, status, comments,
                            "http://localhost:5173/prescriptions/" + prescriptionId);
                }
                case RabbitMQConfig.RK_EVENT_REGISTRATION -> {
                    String eventTitle = String.valueOf(event.getData().get("eventTitle"));
                    smsSimulationService.simulateEventSms(
                            phone, name, eventTitle, "See event page", "",
                            "http://localhost:5173/events");
                }
                case RabbitMQConfig.RK_CONSULTATION_SCHEDULED -> {
                    String doctorName = String.valueOf(event.getData().get("doctorName"));
                    smsSimulationService.simulatePrescriptionSms(
                            phone, name, 0L, "CONSULTATION",
                            "Dr. " + doctorName + " will consult with you.",
                            "http://localhost:5173/chat");
                }
                default -> log.warn("SMS consumer — unknown type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("SMS consumer error — type={}, error={}", event.getType(), e.getMessage());
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
