package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.Puzzle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PuzzleRepository extends JpaRepository<Puzzle, Long> {
    boolean existsByTitle(String title);
    Optional<Puzzle> findByTitle(String title);
    List<Puzzle> findByTheme(String theme);
    List<Puzzle> findByDifficulty(Integer difficulty);
    List<Puzzle> findByThemeAndDifficulty(String theme, Integer difficulty);
    List<Puzzle> findByThemeContainingIgnoreCase(String theme);
    List<Puzzle> findByThemeContainingIgnoreCase(String theme, Sort sort);
    List<Puzzle> findByThemeContainingIgnoreCaseAndDifficulty(String theme, Integer difficulty);
    List<Puzzle> findByThemeContainingIgnoreCaseAndDifficulty(String theme, Integer difficulty, Sort sort);
    Page<Puzzle> findByThemeContainingIgnoreCase(String theme, Pageable pageable);
    Page<Puzzle> findByThemeContainingIgnoreCaseAndDifficulty(String theme, Integer difficulty, Pageable pageable);
    List<Puzzle> findByTheme(String theme, Sort sort);
    List<Puzzle> findByDifficulty(Integer difficulty, Sort sort);
    List<Puzzle> findByThemeAndDifficulty(String theme, Integer difficulty, Sort sort);

    List<Puzzle> findByPublishedTrue();
    List<Puzzle> findByPublishedTrue(Sort sort);
    List<Puzzle> findByPublishedTrueAndTheme(String theme);
    List<Puzzle> findByPublishedTrueAndTheme(String theme, Sort sort);
    List<Puzzle> findByPublishedTrueAndDifficulty(Integer difficulty);
    List<Puzzle> findByPublishedTrueAndDifficulty(Integer difficulty, Sort sort);
    List<Puzzle> findByPublishedTrueAndThemeAndDifficulty(String theme, Integer difficulty);
    List<Puzzle> findByPublishedTrueAndThemeAndDifficulty(String theme, Integer difficulty, Sort sort);
    List<Puzzle> findByPublishedTrueAndThemeContainingIgnoreCase(String theme);
    List<Puzzle> findByPublishedTrueAndThemeContainingIgnoreCase(String theme, Sort sort);
    List<Puzzle> findByPublishedTrueAndThemeContainingIgnoreCaseAndDifficulty(String theme, Integer difficulty);
    List<Puzzle> findByPublishedTrueAndThemeContainingIgnoreCaseAndDifficulty(String theme, Integer difficulty, Sort sort);
    Page<Puzzle> findByPublishedTrue(Pageable pageable);
    Page<Puzzle> findByPublishedTrueAndThemeContainingIgnoreCase(String theme, Pageable pageable);
    Page<Puzzle> findByPublishedTrueAndThemeContainingIgnoreCaseAndDifficulty(String theme, Integer difficulty, Pageable pageable);
    Page<Puzzle> findByPublishedTrueAndDifficulty(Integer difficulty, Pageable pageable);
    Page<Puzzle> findByDifficulty(Integer difficulty, Pageable pageable);
}
