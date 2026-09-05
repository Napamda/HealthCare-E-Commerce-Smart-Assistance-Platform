package org.example.Healthcareplatform.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.notification.dto.NotificationResponse;
import org.example.Healthcareplatform.notification.entity.Notification;
import org.example.Healthcareplatform.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification createNotification(Long userId, String title, String message,
                                           Notification.NotificationType type, Long referenceId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .build();
        Notification saved = notificationRepository.save(notification);
        log.info("Notification created — id={}, userId={}, type={}, refId={}",
                saved.getId(), userId, type, referenceId);
        return saved;
    }

    /**
     * Convenience notification method used by payment/order services.
     */
    @Transactional
    public Notification notify(Long userId, String typeName, String title, String message) {
        if (userId == null) return null;
        Notification.NotificationType type;
        try {
            type = Notification.NotificationType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = Notification.NotificationType.GENERIC;
        }
        return createNotification(userId, title, message, type, null);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));
        notification.setIsRead(true);
        notification = notificationRepository.save(notification);
        return toResponse(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
        log.info("Marked {} notifications as read for userId={}", unread.size(), userId);
    }

    @Transactional
    public void clearAll(Long userId) {
        notificationRepository.deleteByUserId(userId);
        log.info("Cleared all notifications for userId={}", userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType().name())
                .referenceId(n.getReferenceId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
