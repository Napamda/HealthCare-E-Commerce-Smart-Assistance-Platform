package org.example.Healthcareplatform.consultation.repository;

import org.example.Healthcareplatform.consultation.entity.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
