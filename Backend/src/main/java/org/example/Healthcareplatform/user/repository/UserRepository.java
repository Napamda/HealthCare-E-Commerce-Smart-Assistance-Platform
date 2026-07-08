package org.example.Healthcareplatform.user.repository;

import org.example.Healthcareplatform.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByRefreshToken(String refreshToken);

    boolean existsByEmail(String email);
}
