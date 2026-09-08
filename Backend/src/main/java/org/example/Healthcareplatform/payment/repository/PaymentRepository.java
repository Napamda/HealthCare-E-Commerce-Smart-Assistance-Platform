package org.example.Healthcareplatform.payment.repository;

import org.example.Healthcareplatform.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findByReceiptNumber(String receiptNumber);

    Optional<Payment> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, Payment.Status status);

    Optional<Payment> findFirstByOrderIdAndMethodOrderByCreatedAtDesc(Long orderId, Payment.Method method);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
