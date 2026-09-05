package org.example.Healthcareplatform.admin.repository;

import org.example.Healthcareplatform.admin.entity.VendorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorProfileRepository extends JpaRepository<VendorProfile, Long> {

    Optional<VendorProfile> findByUserId(Long userId);

    List<VendorProfile> findByApprovalStatus(VendorProfile.ApprovalStatus status);

    List<VendorProfile> findByApprovalStatusInOrderByCreatedAtDesc(List<VendorProfile.ApprovalStatus> statuses);

    boolean existsByUserId(Long userId);

    long countByApprovalStatus(VendorProfile.ApprovalStatus status);
}
