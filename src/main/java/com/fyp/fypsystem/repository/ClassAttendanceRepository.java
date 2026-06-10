package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.ClassAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClassAttendanceRepository extends JpaRepository<ClassAttendance, Long> {
    List<ClassAttendance> findByClassSlotIdOrderBySessionDateDesc(Long classSlotId);
    Optional<ClassAttendance> findByClassSlotIdAndStudentIdAndSessionDate(Long classSlotId, Long studentId, LocalDate date);
    void deleteByClassSlotId(Long classSlotId);
}
