package org.example.Healthcareplatform.notification.repository;

import org.example.Healthcareplatform.notification.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findAllByOrderByCreatedAtDesc();
    List<EmailLog> findByTypeOrderByCreatedAtDesc(String type);
}
