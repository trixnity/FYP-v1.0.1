package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.ClassApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassApplicationRepository extends JpaRepository<ClassApplication, Long> {
    List<ClassApplication> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<ClassApplication> findByAssignedCoachIdOrderByUpdatedAtDesc(Long assignedCoachId);
    List<ClassApplication> findByStatusOrderByCreatedAtDesc(ClassApplication.ApplicationStatus status);
    List<ClassApplication> findAllByOrderByCreatedAtDesc();
}
