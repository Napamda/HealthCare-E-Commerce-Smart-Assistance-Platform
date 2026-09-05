package org.example.Healthcareplatform.prescription.repository;

import org.example.Healthcareplatform.prescription.entity.Prescription;
import org.example.Healthcareplatform.prescription.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

    List<PrescriptionItem> findByPrescriptionIdOrderByIdAsc(Long prescriptionId);

    void deleteByPrescriptionId(Long prescriptionId);

    /**
     * True when the patient has an APPROVED prescription that includes the
     * given product — used to gate prescription-required products in the cart.
     */
    @Query("SELECT COUNT(pi) > 0 FROM PrescriptionItem pi WHERE pi.productId = :productId "
            + "AND pi.prescriptionId IN ("
            + "  SELECT p.id FROM Prescription p WHERE p.patientUserId = :patientUserId "
            + "  AND p.status = :status)")
    boolean existsApprovedForProduct(@Param("productId") Long productId,
                                     @Param("patientUserId") Long patientUserId,
                                     @Param("status") Prescription.PrescriptionStatus status);
}
