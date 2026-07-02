package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.SavedGame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedGameRepository extends JpaRepository<SavedGame, Long> {
    List<SavedGame> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<SavedGame> findByIdAndUserId(Long id, Long userId);
    List<SavedGame> findByOwnerEmailOrderByUpdatedAtDesc(String ownerEmail);
    Optional<SavedGame> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
