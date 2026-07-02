package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.SessionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SessionPlanRepository extends JpaRepository<SessionPlan, Long> {
    List<SessionPlan> findByCoachIdOrderByCreatedAtDesc(Long coachId);
    List<SessionPlan> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    Optional<SessionPlan> findByCoachIdAndStudentIdAndMonth(Long coachId, Long studentId, String month);
    boolean existsByCoachIdAndStudentId(Long coachId, Long studentId);
}
