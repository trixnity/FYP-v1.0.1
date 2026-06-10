package com.fyp.fypsystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long coachId;       // User.id of the coach who assigned this
    private Long studentId;     // User.id of the student it's assigned to
    private Long puzzleId;      // Puzzle.id being assigned

    private String puzzleTitle; // snapshot so it's readable without joining
    private String puzzleTheme;

    private LocalDateTime assignedAt;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    private AssignmentStatus status = AssignmentStatus.PENDING;

    public enum AssignmentStatus {
        PENDING, COMPLETED, OVERDUE
    }

    @PrePersist
    public void prePersist() {
        this.assignedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }

    public Long getCoachId() { return coachId; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getPuzzleId() { return puzzleId; }
    public void setPuzzleId(Long puzzleId) { this.puzzleId = puzzleId; }

    public String getPuzzleTitle() { return puzzleTitle; }
    public void setPuzzleTitle(String puzzleTitle) { this.puzzleTitle = puzzleTitle; }

    public String getPuzzleTheme() { return puzzleTheme; }
    public void setPuzzleTheme(String puzzleTheme) { this.puzzleTheme = puzzleTheme; }

    public LocalDateTime getAssignedAt() { return assignedAt; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }
}
