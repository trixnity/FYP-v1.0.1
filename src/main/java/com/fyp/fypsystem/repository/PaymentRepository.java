package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Payment> findByCoachIdOrderByCreatedAtDesc(Long coachId);
    Optional<Payment> findBySessionPlanId(Long sessionPlanId);
    List<Payment> findByStudentIdAndStatus(Long studentId, String status);
}
