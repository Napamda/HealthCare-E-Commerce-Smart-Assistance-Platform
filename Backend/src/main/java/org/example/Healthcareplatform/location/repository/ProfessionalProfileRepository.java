package org.example.Healthcareplatform.location.repository;

import org.example.Healthcareplatform.location.entity.ProfessionalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessionalProfileRepository extends JpaRepository<ProfessionalProfile, Long> {

    List<ProfessionalProfile> findByActiveTrueOrderByLastNameAsc();

    List<ProfessionalProfile> findBySpecialtyContainingIgnoreCaseAndActiveTrueOrderByLastNameAsc(String specialty);

    Optional<ProfessionalProfile> findByUserId(Long userId);

    @Query("select distinct p.specialty from ProfessionalProfile p " +
            "where p.active = true and p.specialty is not null order by p.specialty")
    List<String> findDistinctSpecialtyByActiveTrueOrderBySpecialtyAsc();
}
