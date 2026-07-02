package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.Role;
import com.fyp.fypsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByChessUsername(String chessUsername);
    List<User> findByRole(Role role);
    List<User> findByCoachIdAndRole(Long coachId, Role role);
    List<User> findByCoachId(Long coachId);
}
