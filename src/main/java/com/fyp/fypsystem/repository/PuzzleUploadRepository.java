package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.PuzzleUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PuzzleUploadRepository extends JpaRepository<PuzzleUpload, Long> {
    List<PuzzleUpload> findAllByOrderByUploadedAtDesc();
}
