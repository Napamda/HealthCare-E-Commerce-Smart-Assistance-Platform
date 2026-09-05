package org.example.Healthcareplatform.notification.repository;

import org.example.Healthcareplatform.notification.entity.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {
    List<SmsLog> findAllByOrderByCreatedAtDesc();
    List<SmsLog> findByTypeOrderByCreatedAtDesc(String type);
}
