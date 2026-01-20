package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findByUserId(Long userId);
    List<Attempt> findByUserEmail(String userEmail);
    List<Attempt> findByUserIdAndTheme(Long userId, String theme);
}
