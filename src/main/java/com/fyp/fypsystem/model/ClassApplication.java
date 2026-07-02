package com.fyp.fypsystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_applications")
public class ClassApplication {

    public enum ApplicationStatus {
        PENDING,
        APPROVED,
        ASSIGNED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;
    private String studentName;
    private String email;
    private String phone;
    private Integer age;
    private String chessLevel;
    private Long preferredCoachId;
    private String preferredCoachName;
    private String preferredClassType;
    private String preferredSchedule;
    private String goal;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Long assignedCoachId;
    private String assignedCoachName;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = ApplicationStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getChessLevel() { return chessLevel; }
    public void setChessLevel(String chessLevel) { this.chessLevel = chessLevel; }
    public Long getPreferredCoachId() { return preferredCoachId; }
    public void setPreferredCoachId(Long preferredCoachId) { this.preferredCoachId = preferredCoachId; }
    public String getPreferredCoachName() { return preferredCoachName; }
    public void setPreferredCoachName(String preferredCoachName) { this.preferredCoachName = preferredCoachName; }
    public String getPreferredClassType() { return preferredClassType; }
    public void setPreferredClassType(String preferredClassType) { this.preferredClassType = preferredClassType; }
    public String getPreferredSchedule() { return preferredSchedule; }
    public void setPreferredSchedule(String preferredSchedule) { this.preferredSchedule = preferredSchedule; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getAssignedCoachId() { return assignedCoachId; }
    public void setAssignedCoachId(Long assignedCoachId) { this.assignedCoachId = assignedCoachId; }
    public String getAssignedCoachName() { return assignedCoachName; }
    public void setAssignedCoachName(String assignedCoachName) { this.assignedCoachName = assignedCoachName; }
    public ApplicationStatus getStatus() { return status == null ? ApplicationStatus.PENDING : status; }
    public void setStatus(ApplicationStatus status) { this.status = status == null ? ApplicationStatus.PENDING : status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
