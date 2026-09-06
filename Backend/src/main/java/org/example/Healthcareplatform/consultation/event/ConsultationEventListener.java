package org.example.Healthcareplatform.consultation.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.notification.entity.Notification;
import org.example.Healthcareplatform.notification.service.NotificationService;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.entity.UserRole;
import org.example.Healthcareplatform.user.entity.UserStatus;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @EventListener
    public void onConsultationCreated(ConsultationCreatedEvent event) {
        log.info("Handling consultation created event — consultationId={}, patientUserId={}",
                event.getConsultationId(), event.getPatientUserId());

        // Tell the patient their chat is being reviewed.
        try {
            notificationService.createNotification(
                    event.getPatientUserId(),
                    "New consultation request",
                    "Your chat was escalated to a consultation (priority " + event.getPriority() + ").",
                    Notification.NotificationType.CONSULTATION_CREATED,
                    event.getConsultationId());
        } catch (Exception e) {
            log.warn("Failed to notify patient userId={} about consultation {}: {}",
                    event.getPatientUserId(), event.getConsultationId(), e.getMessage());
        }

        // Tell every active doctor a new request is waiting in the queue.
        List<User> doctors = userRepository.findByRoleAndStatusAndEmailVerifiedTrue(
                UserRole.DOCTOR, UserStatus.ACTIVE);
        for (User doctor : doctors) {
            try {
                notificationService.createNotification(
                        doctor.getId(),
                        "New consultation request",
                        "A patient escalated a chat to the queue (priority " + event.getPriority()
                                + ", request #" + event.getConsultationId() + ")."
                                + " Open your queue to accept.",
                        Notification.NotificationType.CONSULTATION_AVAILABLE,
                        event.getConsultationId());
            } catch (Exception e) {
                log.warn("Failed to notify doctor userId={} about consultation {}: {}",
                        doctor.getId(), event.getConsultationId(), e.getMessage());
            }
        }
    }
}
