package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByCoachId(Long coachId);
    Optional<Student> findByEmail(String email);
}
