package org.example.Healthcareplatform.location.repository;

import org.example.Healthcareplatform.location.entity.ProfessionalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessionalProfileRepository extends JpaRepository<ProfessionalProfile, Long> {

    List<ProfessionalProfile> findByActiveTrueOrderByLastNameAsc();

    List<ProfessionalProfile> findBySpecialtyContainingIgnoreCaseAndActiveTrueOrderByLastNameAsc(String specialty);

    List<ProfessionalProfile> findByActiveTrueAndLatitudeIsNotNullAndLongitudeIsNotNull();

    Optional<ProfessionalProfile> findByUserId(Long userId);

    List<String> findDistinctSpecialtyByActiveTrueOrderBySpecialtyAsc();
}
