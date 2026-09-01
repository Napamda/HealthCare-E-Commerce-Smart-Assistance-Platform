package org.example.Healthcareplatform.event.registration.repository;

import org.example.Healthcareplatform.event.registration.entity.EventRegistration;
import org.example.Healthcareplatform.event.registration.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    List<EventRegistration> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, RegistrationStatus status);

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);
}
