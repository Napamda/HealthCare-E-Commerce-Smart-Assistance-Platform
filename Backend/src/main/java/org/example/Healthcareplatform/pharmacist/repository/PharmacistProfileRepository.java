package org.example.Healthcareplatform.pharmacist.repository;

import org.example.Healthcareplatform.pharmacist.entity.PharmacistProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PharmacistProfileRepository extends JpaRepository<PharmacistProfile, Long> {

    Optional<PharmacistProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}