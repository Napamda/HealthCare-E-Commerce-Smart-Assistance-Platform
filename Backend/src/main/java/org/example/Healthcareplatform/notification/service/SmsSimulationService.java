package org.example.Healthcareplatform.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.notification.entity.SmsLog;
import org.example.Healthcareplatform.notification.repository.SmsLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsSimulationService {

    private final SpringTemplateEngine templateEngine;
    private final SmsLogRepository smsLogRepository;

    /**
     * Simulate an order confirmation SMS.
     */
    @Async
    public void simulateOrderSms(String phone, String name, Long orderId,
                                  String total, String trackingUrl) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        ctx.setVariable("orderId", orderId);
        ctx.setVariable("total", total);
        ctx.setVariable("trackingUrl", trackingUrl);
        String text = templateEngine.process("sms/order-confirmation", ctx);
        saveSmsLog(phone, text, "ORDER_CONFIRMATION");
    }

    /**
     * Simulate a prescription status SMS.
     */
    @Async
    public void simulatePrescriptionSms(String phone, String name, Long prescriptionId,
                                        String status, String comments, String prescriptionUrl) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        ctx.setVariable("prescriptionId", prescriptionId);
        ctx.setVariable("status", status);
        ctx.setVariable("comments", comments != null ? comments : "");
        ctx.setVariable("prescriptionUrl", prescriptionUrl);
        String text = templateEngine.process("sms/prescription-status", ctx);
        saveSmsLog(phone, text, "PRESCRIPTION_STATUS");
    }

    /**
     * Simulate an event reminder SMS.
     */
    @Async
    public void simulateEventSms(String phone, String name, String eventTitle,
                                  String eventDate, String venue, String eventUrl) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        ctx.setVariable("eventTitle", eventTitle);
        ctx.setVariable("eventDate", eventDate);
        ctx.setVariable("venue", venue != null ? " at " + venue : "");
        ctx.setVariable("eventUrl", eventUrl);
        String text = templateEngine.process("sms/event-reminder", ctx);
        saveSmsLog(phone, text, "EVENT_REMINDER");
    }

    private void saveSmsLog(String phone, String message, String type) {
        SmsLog smsLog = SmsLog.builder()
                .phoneNumber(phone)
                .message(message)
                .type(type)
                .status("SENT")
                .build();
        smsLogRepository.save(smsLog);
        log.info("SMS simulated — phone={}, type={}, message={}", phone, type, message);
    }
}
