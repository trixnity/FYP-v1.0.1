package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.ClassEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {
    List<ClassEnrollment> findByClassSlotId(Long classSlotId);
    Optional<ClassEnrollment> findByClassSlotIdAndStudentId(Long classSlotId, Long studentId);
    void deleteByClassSlotId(Long classSlotId);
}
