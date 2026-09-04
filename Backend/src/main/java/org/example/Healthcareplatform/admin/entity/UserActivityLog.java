package org.example.Healthcareplatform.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit trail entry for a user account. Records lifecycle events
 * (registration, login) as well as admin moderation actions
 * (suspension, activation, role change) — Task 3.1 "User activity log".
 */
@Entity
@Table(name = "user_activity_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user the activity concerns. */
    @Column(nullable = false)
    private Long userId;

    /**
     * The admin who performed the action. Null for system/self events
     * such as registration and login.
     */
    private Long actorAdminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityAction action;

    @Column(length = 255)
    private String details;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ActivityAction {
        REGISTERED,
        LOGIN,
        SUSPENDED,
        ACTIVATED,
        ROLE_CHANGED
    }
}
