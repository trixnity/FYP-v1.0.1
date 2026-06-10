package com.fyp.fypsystem.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_attendance")
public class ClassAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long classSlotId;
    private Long studentId;
    private String studentName;
    private LocalDate sessionDate;
    private Boolean present;
    private LocalDateTime recordedAt;

    @PrePersist
    void prePersist() {
        this.recordedAt = LocalDateTime.now();
        if (this.present == null) this.present = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClassSlotId() { return classSlotId; }
    public void setClassSlotId(Long classSlotId) { this.classSlotId = classSlotId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public Boolean getPresent() { return present; }
    public void setPresent(Boolean present) { this.present = present; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
