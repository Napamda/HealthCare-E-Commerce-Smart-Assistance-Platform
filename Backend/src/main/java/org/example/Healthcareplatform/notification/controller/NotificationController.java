package org.example.Healthcareplatform.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.common.CurrentUser;
import org.example.Healthcareplatform.notification.dto.NotificationResponse;
import org.example.Healthcareplatform.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // ============ Original endpoints (path-based userId) ============

    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotifications(@PathVariable Long userId) {
        log.info("Get all notifications — userId={}", userId);
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    @GetMapping("/{userId}/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(@PathVariable Long userId) {
        log.info("Get unread notifications — userId={}", userId);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        log.info("Mark notification as read — id={}", id);
        return ResponseEntity.ok(notificationService.markAsRead(CurrentUser.userId(), id));
    }

    @PatchMapping("/{userId}/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(@PathVariable Long userId) {
        log.info("Mark all notifications as read — userId={}", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    // ============ Current-user endpoints (used by the notification bell) ============

    @GetMapping("/me")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        Long userId = CurrentUser.userId();
        log.info("Get my notifications — userId={}", userId);
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<Map<String, Long>> getMyUnreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(CurrentUser.userId())));
    }

    @PostMapping("/me/read-all")
    public ResponseEntity<Map<String, String>> markAllReadMe() {
        notificationService.markAllAsRead(CurrentUser.userId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @PostMapping("/me/{id}/read")
    public ResponseEntity<NotificationResponse> markOneReadMe(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(CurrentUser.userId(), id));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> clearAllMe() {
        notificationService.clearAll(CurrentUser.userId());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeError(RuntimeException e) {
        log.error("Notification error: {}", e.getMessage(), e);
        if (e.getMessage() != null && e.getMessage().contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
    }
}
