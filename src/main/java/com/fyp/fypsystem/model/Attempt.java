package com.fyp.fypsystem.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "attempts")
public class Attempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long puzzleId;
    private Long userId;
    private String userEmail;
    private boolean solved;
    private int stars; // 1-3 based on tries
    private int attempts;
    private Integer timeSeconds;
    private Integer performanceScore;
    private Boolean firstAttemptSolved;
    private String theme;
    private LocalDateTime solvedAt;

    public Attempt() {}

    public Attempt(Long puzzleId, Long userId, String userEmail, boolean solved, int stars, int attempts, String theme, LocalDateTime solvedAt) {
        this.puzzleId = puzzleId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.solved = solved;
        this.stars = stars;
        this.attempts = attempts;
        this.theme = theme;
        this.solvedAt = solvedAt;
    }

    public Long getId() { return id; }
    public Long getPuzzleId() { return puzzleId; }
    public void setPuzzleId(Long puzzleId) { this.puzzleId = puzzleId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }
    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Integer getTimeSeconds() { return timeSeconds; }
    public void setTimeSeconds(Integer timeSeconds) { this.timeSeconds = timeSeconds; }
    public Integer getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(Integer performanceScore) { this.performanceScore = performanceScore; }
    public Boolean getFirstAttemptSolved() { return firstAttemptSolved; }
    public void setFirstAttemptSolved(Boolean firstAttemptSolved) { this.firstAttemptSolved = firstAttemptSolved; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public LocalDateTime getSolvedAt() { return solvedAt; }
    public void setSolvedAt(LocalDateTime solvedAt) { this.solvedAt = solvedAt; }
}
