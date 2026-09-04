package org.example.Healthcareplatform.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.Healthcareplatform.admin.entity.UserActivityLog;
import org.example.Healthcareplatform.admin.repository.UserActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes user activity / audit entries. Kept as a tiny separate service
 * so both the admin module and the auth flow (registration, login) can
 * record events without circular dependencies.
 */
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final UserActivityLogRepository activityLogRepository;

    @Transactional
    public void record(Long userId, Long actorAdminId,
                       UserActivityLog.ActivityAction action, String details) {
        activityLogRepository.save(UserActivityLog.builder()
                .userId(userId)
                .actorAdminId(actorAdminId)
                .action(action)
                .details(details)
                .build());
    }
}
