package org.example.Healthcareplatform.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.admin.dto.ActivityLogResponse;
import org.example.Healthcareplatform.admin.dto.AdminUserResponse;
import org.example.Healthcareplatform.admin.dto.PagedResponse;
import org.example.Healthcareplatform.admin.entity.UserActivityLog;
import org.example.Healthcareplatform.admin.repository.UserActivityLogRepository;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.entity.UserRole;
import org.example.Healthcareplatform.user.entity.UserStatus;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserActivityLogRepository activityLogRepository;
    private final ActivityLogService activityLogService;

    // ---------------------------------------------------------------
    // Task 3.1 — View all users + search/filter
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public PagedResponse<AdminUserResponse> listUsers(String search, String role, String status,
                                                      int page, int size) {
        UserRole roleFilter = parseRole(role);
        UserStatus statusFilter = parseStatus(status);
        String searchTerm = (search == null || search.isBlank()) ? null : search.trim();

        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<AdminUserResponse> result = userRepository
                .searchUsers(searchTerm, roleFilter, statusFilter, pageable)
                .map(this::toUserResponse);
        return PagedResponse.of(result);
    }

    // ---------------------------------------------------------------
    // Task 3.1 — Suspend / activate
    // ---------------------------------------------------------------

    @Transactional
    public AdminUserResponse updateStatus(Long adminId, Long userId, String rawStatus, String reason) {
        UserStatus targetStatus = parseStatus(rawStatus);
        if (targetStatus == null) {
            throw new IllegalArgumentException("status must be ACTIVE or SUSPENDED");
        }

        User user = requireUser(userId);
        assertNotSelf(adminId, userId, "change the status of your own account");
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Administrator accounts cannot be suspended or activated here");
        }

        String trimmedReason = reason == null ? null : reason.trim();

        if (targetStatus == UserStatus.SUSPENDED) {
            user.setStatus(UserStatus.SUSPENDED);
            user.setSuspendedReason(trimmedReason);
            user.setSuspendedAt(java.time.LocalDateTime.now());
            // Revoke active sessions — the refresh token no longer works, so
            // the suspended user is fully logged out once their access token expires.
            user.setRefreshToken(null);
            activityLogService.record(userId, adminId, UserActivityLog.ActivityAction.SUSPENDED,
                    trimmedReason == null || trimmedReason.isBlank() ? "Account suspended" : trimmedReason);
            log.info("User suspended — adminId={}, userId={}, reason={}", adminId, userId, trimmedReason);
        } else {
            if (user.getStatus() == UserStatus.ACTIVE && user.getSuspendedAt() == null) {
                throw new IllegalArgumentException("User is already active");
            }
            user.setStatus(UserStatus.ACTIVE);
            user.setSuspendedReason(null);
            user.setSuspendedAt(null);
            activityLogService.record(userId, adminId, UserActivityLog.ActivityAction.ACTIVATED,
                    "Account reactivated");
            log.info("User activated — adminId={}, userId={}", adminId, userId);
        }

        return toUserResponse(userRepository.save(user));
    }

    // ---------------------------------------------------------------
    // Task 3.1 — Change user role
    // ---------------------------------------------------------------

    @Transactional
    public AdminUserResponse changeRole(Long adminId, Long userId, String rawRole) {
        UserRole newRole = parseRole(rawRole);
        if (newRole == null) {
            throw new IllegalArgumentException("Invalid role: " + rawRole
                    + ". Must be PATIENT, DOCTOR, PHARMACIST, or VENDOR");
        }
        if (newRole == UserRole.ADMIN) {
            throw new IllegalArgumentException("The ADMIN role cannot be assigned from this screen");
        }

        User user = requireUser(userId);
        assertNotSelf(adminId, userId, "change your own role");
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Administrator roles cannot be modified");
        }
        if (user.getRole() == newRole) {
            throw new IllegalArgumentException("User already has role " + newRole.name());
        }

        UserRole oldRole = user.getRole();
        user.setRole(newRole);
        activityLogService.record(userId, adminId, UserActivityLog.ActivityAction.ROLE_CHANGED,
                "Role changed: " + oldRole.name() + " → " + newRole.name());
        log.info("User role changed — adminId={}, userId={}, {} → {}", adminId, userId, oldRole, newRole);

        return toUserResponse(userRepository.save(user));
    }

    // ---------------------------------------------------------------
    // Task 3.1 — User activity log
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public PagedResponse<ActivityLogResponse> getActivityForUser(Long userId, int page, int size) {
        requireUser(userId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<ActivityLogResponse> result = activityLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toLogResponse);
        return PagedResponse.of(result);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityLogResponse> getRecentActivity(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<ActivityLogResponse> result = activityLogRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toLogResponse);
        return PagedResponse.of(result);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
    }

    private void assertNotSelf(Long adminId, Long targetUserId, String action) {
        if (adminId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot " + action);
        }
    }

    private int clampSize(int size) {
        if (size <= 0) return 20;
        return Math.min(size, 100);
    }

    private UserRole parseRole(String role) {
        if (role == null || role.isBlank()) return null;
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role filter: " + role);
        }
    }

    private UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status filter: " + status);
        }
    }

    private AdminUserResponse toUserResponse(User u) {
        return AdminUserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .role(u.getRole().name())
                .status(u.getStatus().name())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .emailVerified(u.isEmailVerified())
                .suspendedReason(u.getSuspendedReason())
                .suspendedAt(u.getSuspendedAt())
                .createdAt(u.getCreatedAt())
                .build();
    }

    private ActivityLogResponse toLogResponse(UserActivityLog l) {
        return ActivityLogResponse.builder()
                .id(l.getId())
                .userId(l.getUserId())
                .actorAdminId(l.getActorAdminId())
                .action(l.getAction().name())
                .details(l.getDetails())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
