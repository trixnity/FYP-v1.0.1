package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.Opening;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OpeningRepository extends JpaRepository<Opening, Long> {
    Optional<Opening> findByMoveSequence(String moveSequence);
}
