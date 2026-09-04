package org.example.Healthcareplatform.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the Healthcare Platform.
 *
 * Exchange:  healthcare.events  (topic)
 * Queues (bound with routing keys):
 *   healthcare.notification.queue  → order.created, payment.success, prescription.approved,
 *                                    event.registration, consultation.scheduled, user.registered
 *   healthcare.inventory.queue     → order.created, payment.success
 *   healthcare.email.queue         → order.created, payment.success, prescription.approved,
 *                                    event.registration, consultation.scheduled, user.registered
 *   healthcare.sms.queue           → order.created, payment.success, prescription.approved,
 *                                    event.registration, consultation.scheduled
 * Dead-letter:
 *   healthcare.dlx  (direct) → healthcare.dlq
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "healthcare.events";

    public static final String NOTIFICATION_QUEUE = "healthcare.notification.queue";
    public static final String INVENTORY_QUEUE    = "healthcare.inventory.queue";
    public static final String EMAIL_QUEUE        = "healthcare.email.queue";
    public static final String SMS_QUEUE          = "healthcare.sms.queue";

    public static final String DLX   = "healthcare.dlx";
    public static final String DLQ   = "healthcare.dlq";

    // ---- Routing keys ----
    public static final String RK_ORDER_CREATED       = "order.created";
    public static final String RK_PAYMENT_SUCCESS      = "payment.success";
    public static final String RK_PRESCRIPTION_APPROVED = "prescription.approved";
    public static final String RK_EVENT_REGISTRATION   = "event.registration";
    public static final String RK_CONSULTATION_SCHEDULED = "consultation.scheduled";
    public static final String RK_USER_REGISTERED      = "user.registered";

    // ---- Dead-letter exchange & queue ----
    @Bean
    DirectExchange dlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue dlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding dlqBinding() {
        return BindingBuilder.bind(dlq()).to(dlx()).with("");
    }

    // ---- Main topic exchange ----
    @Bean
    TopicExchange healthcareExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // ---- Helper: queue with dead-letter config ----
    private Queue buildQueue(String name) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", DLX)
                .build();
    }

    // ---- Notification queue ----
    @Bean
    Queue notificationQueue() {
        return buildQueue(NOTIFICATION_QUEUE);
    }

    @Bean
    Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(healthcareExchange())
                .with(RK_ORDER_CREATED);
    }

    @Bean
    Binding notificationBindingPayment() {
        return BindingBuilder.bind(notificationQueue())
                .to(healthcareExchange())
                .with(RK_PAYMENT_SUCCESS);
    }

    @Bean
    Binding notificationBindingPrescription() {
        return BindingBuilder.bind(notificationQueue())
                .to(healthcareExchange())
                .with(RK_PRESCRIPTION_APPROVED);
    }

    @Bean
    Binding notificationBindingEvent() {
        return BindingBuilder.bind(notificationQueue())
                .to(healthcareExchange())
                .with(RK_EVENT_REGISTRATION);
    }

    @Bean
    Binding notificationBindingConsultation() {
        return BindingBuilder.bind(notificationQueue())
                .to(healthcareExchange())
                .with(RK_CONSULTATION_SCHEDULED);
    }

    @Bean
    Binding notificationBindingUserRegistered() {
        return BindingBuilder.bind(notificationQueue())
                .to(healthcareExchange())
                .with(RK_USER_REGISTERED);
    }

    // ---- Inventory queue ----
    @Bean
    Queue inventoryQueue() {
        return buildQueue(INVENTORY_QUEUE);
    }

    @Bean
    Binding inventoryBindingOrder() {
        return BindingBuilder.bind(inventoryQueue())
                .to(healthcareExchange())
                .with(RK_ORDER_CREATED);
    }

    @Bean
    Binding inventoryBindingPayment() {
        return BindingBuilder.bind(inventoryQueue())
                .to(healthcareExchange())
                .with(RK_PAYMENT_SUCCESS);
    }

    // ---- Email queue ----
    @Bean
    Queue emailQueue() {
        return buildQueue(EMAIL_QUEUE);
    }

    @Bean
    Binding emailBindingOrder() {
        return BindingBuilder.bind(emailQueue())
                .to(healthcareExchange())
                .with(RK_ORDER_CREATED);
    }

    @Bean
    Binding emailBindingPayment() {
        return BindingBuilder.bind(emailQueue())
                .to(healthcareExchange())
                .with(RK_PAYMENT_SUCCESS);
    }

    @Bean
    Binding emailBindingPrescription() {
        return BindingBuilder.bind(emailQueue())
                .to(healthcareExchange())
                .with(RK_PRESCRIPTION_APPROVED);
    }

    @Bean
    Binding emailBindingEvent() {
        return BindingBuilder.bind(emailQueue())
                .to(healthcareExchange())
                .with(RK_EVENT_REGISTRATION);
    }

    @Bean
    Binding emailBindingConsultation() {
        return BindingBuilder.bind(emailQueue())
                .to(healthcareExchange())
                .with(RK_CONSULTATION_SCHEDULED);
    }

    @Bean
    Binding emailBindingUserRegistered() {
        return BindingBuilder.bind(emailQueue())
                .to(healthcareExchange())
                .with(RK_USER_REGISTERED);
    }

    // ---- SMS queue ----
    @Bean
    Queue smsQueue() {
        return buildQueue(SMS_QUEUE);
    }

    @Bean
    Binding smsBindingOrder() {
        return BindingBuilder.bind(smsQueue())
                .to(healthcareExchange())
                .with(RK_ORDER_CREATED);
    }

    @Bean
    Binding smsBindingPayment() {
        return BindingBuilder.bind(smsQueue())
                .to(healthcareExchange())
                .with(RK_PAYMENT_SUCCESS);
    }

    @Bean
    Binding smsBindingPrescription() {
        return BindingBuilder.bind(smsQueue())
                .to(healthcareExchange())
                .with(RK_PRESCRIPTION_APPROVED);
    }

    @Bean
    Binding smsBindingEvent() {
        return BindingBuilder.bind(smsQueue())
                .to(healthcareExchange())
                .with(RK_EVENT_REGISTRATION);
    }

    @Bean
    Binding smsBindingConsultation() {
        return BindingBuilder.bind(smsQueue())
                .to(healthcareExchange())
                .with(RK_CONSULTATION_SCHEDULED);
    }

    // ---- JSON message converter ----
    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
