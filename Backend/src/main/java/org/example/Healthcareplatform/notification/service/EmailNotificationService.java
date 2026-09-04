package org.example.Healthcareplatform.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.notification.entity.EmailLog;
import org.example.Healthcareplatform.notification.repository.EmailLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final EmailLogRepository emailLogRepository;

    @Value("${spring.mail.host:localhost}")
    private String mailHost;

    @Value("${spring.mail.port:1025}")
    private int mailPort;

    /**
     * Send a welcome email to a newly registered user.
     */
    @Async
    public void sendWelcomeEmail(String to, String name, String dashboardUrl) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        ctx.setVariable("dashboardUrl", dashboardUrl);
        String html = templateEngine.process("email/welcome", ctx);
        sendHtmlEmail(to, "Welcome to HealthCare Platform!", html, "WELCOME");
    }

    /**
     * Send an order confirmation email.
     */
    @Async
    public void sendOrderConfirmationEmail(String to, String name, Long orderId,
                                            List<Map<String, Object>> items, String total) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        ctx.setVariable("orderId", orderId);
        ctx.setVariable("items", items);
        ctx.setVariable("total", total);
        String html = templateEngine.process("email/order-confirmation", ctx);
        sendHtmlEmail(to, "Order Confirmed — #" + orderId, html, "ORDER_CONFIRMATION");
    }

    /**
     * Send a prescription status update email.
     */
    @Async
    public void sendPrescriptionStatusEmail(String to, String name, Long prescriptionId,
                                             String status, String comments, String prescriptionUrl) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        ctx.setVariable("prescriptionId", prescriptionId);
        ctx.setVariable("status", status);
        ctx.setVariable("comments", comments);
        ctx.setVariable("prescriptionUrl", prescriptionUrl);
        String html = templateEngine.process("email/prescription-status", ctx);
        sendHtmlEmail(to, "Prescription " + status + " — #" + prescriptionId, html, "PRESCRIPTION_STATUS");
    }

    /**
     * Send an event reminder email.
     */
    @Async
    public void sendEventReminderEmail(String to, String name, String eventTitle,
                                       String eventDate, String venue, String eventUrl) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        ctx.setVariable("eventTitle", eventTitle);
        ctx.setVariable("eventDate", eventDate);
        ctx.setVariable("venue", venue);
        ctx.setVariable("eventUrl", eventUrl);
        String html = templateEngine.process("email/event-reminder", ctx);
        sendHtmlEmail(to, "Event Reminder: " + eventTitle, html, "EVENT_REMINDER");
    }

    /**
     * Render a Thymeleaf template, send the email via JavaMailSender, and
     * persist an EmailLog entry (QUEUED → SENT / FAILED).
     */
    private void sendHtmlEmail(String to, String subject, String html, String type) {
        EmailLog logEntry = EmailLog.builder()
                .toEmail(to)
                .subject(subject)
                .body(html)
                .type(type)
                .status("QUEUED")
                .build();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@healthcare.com", "HealthCare Platform");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);

            logEntry.setStatus("SENT");
            log.info("Email sent — to={}, subject={}, type={}", to, subject, type);
        } catch (Exception e) {
            logEntry.setStatus("FAILED");
            logEntry.setError(e.getMessage());
            log.warn("Email failed — to={}, subject={}, error={}", to, subject, e.getMessage());
        }

        emailLogRepository.save(logEntry);
    }

    /**
     * Check whether SMTP is configured (not just localhost:1025 default).
     */
    public boolean isSmtpConfigured() {
        return mailHost != null && !mailHost.isBlank();
    }
}
