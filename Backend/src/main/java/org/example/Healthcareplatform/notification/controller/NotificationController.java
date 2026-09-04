package org.example.Healthcareplatform.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.notification.dto.NotificationResponse;
import org.example.Healthcareplatform.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /** Patient/doctor reads their own notifications — id from auth token. */
    @GetMapping("/mine")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            org.springframework.security.core.Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        log.info("Get all notifications — userId={}", userId);
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    @GetMapping("/mine/unread")
    public ResponseEntity<List<NotificationResponse>> getMyUnreadNotifications(
            org.springframework.security.core.Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/mine/unread-count")
    public ResponseEntity<Map<String, Long>> getMyUnreadCount(
            org.springframework.security.core.Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        log.info("Mark notification as read — id={}", id);
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PatchMapping("/mine/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            org.springframework.security.core.Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        log.info("Mark all notifications as read — userId={}", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
