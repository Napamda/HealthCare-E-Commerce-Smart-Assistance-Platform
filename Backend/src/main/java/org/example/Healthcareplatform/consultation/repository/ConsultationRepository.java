package org.example.Healthcareplatform.consultation.repository;

import jakarta.persistence.LockModeType;
import org.example.Healthcareplatform.consultation.entity.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByPatientUserIdOrderByCreatedAtDesc(Long patientUserId);

    Page<Consultation> findByPatientUserIdOrderByCreatedAtDesc(Long patientUserId, Pageable pageable);

    List<Consultation> findByStatusOrderByCreatedAtAsc(Consultation.ConsultationStatus status);

    Page<Consultation> findByDoctorUserIdOrDoctorUserIdIsNullOrderByCreatedAtDesc(
            Long doctorUserId, Pageable pageable);

    boolean existsByConversationIdAndStatusNot(Long conversationId, Consultation.ConsultationStatus status);

    List<Consultation> findAllByOrderByCreatedAtDesc();

    List<Consultation> findByStatusInOrderByPriorityAscCreatedAtAsc(List<Consultation.ConsultationStatus> statuses);

    List<Consultation> findByStatusOrderByScheduledAtAscCreatedAtDesc(Consultation.ConsultationStatus status);

    List<Consultation> findByStatusInAndDoctorUserIdOrderByScheduledAtAscCreatedAtDesc(
            List<Consultation.ConsultationStatus> statuses, Long doctorUserId);

    Optional<Consultation> findFirstByConversationIdAndStatusIn(
            Long conversationId, List<Consultation.ConsultationStatus> statuses);

    List<Consultation> findByDoctorUserIdAndStatusInOrderByUpdatedAtDesc(
            Long doctorUserId, List<Consultation.ConsultationStatus> statuses);

    // Shared pool: requests nobody has claimed yet.
    List<Consultation> findByDoctorUserIdIsNullAndStatusOrderByPriorityAscCreatedAtAsc(
            Consultation.ConsultationStatus status);

    // A doctor's own active cases (accepted / in progress).
    List<Consultation> findByDoctorUserIdAndStatusInOrderByPriorityAscCreatedAtAsc(
            Long doctorUserId, List<Consultation.ConsultationStatus> statuses);

    // Row-level lock so two doctors cannot accept the same request concurrently.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Consultation c WHERE c.id = :id")
    Optional<Consultation> findByIdForUpdate(@Param("id") Long id);
}
