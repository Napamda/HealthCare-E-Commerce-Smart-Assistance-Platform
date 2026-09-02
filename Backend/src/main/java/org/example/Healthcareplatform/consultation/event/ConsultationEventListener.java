package org.example.Healthcareplatform.consultation.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.notification.entity.Notification;
import org.example.Healthcareplatform.notification.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultationEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void onConsultationCreated(ConsultationCreatedEvent event) {
        log.info("Handling consultation created event — consultationId={}, patientUserId={}",
                event.getConsultationId(), event.getPatientUserId());
        notificationService.createNotification(
                event.getPatientUserId(),
                "New consultation request",
                "Your chat was escalated to a consultation (priority " + event.getPriority() + ").",
                Notification.NotificationType.CONSULTATION_CREATED,
                event.getConsultationId());
    }
}
