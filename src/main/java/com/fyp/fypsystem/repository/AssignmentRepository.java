package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByStudentId(Long studentId);
    List<Assignment> findByCoachId(Long coachId);
    List<Assignment> findByStudentIdAndStatus(Long studentId, Assignment.AssignmentStatus status);
}
