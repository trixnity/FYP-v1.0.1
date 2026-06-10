package com.fyp.fypsystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_enrollments")
public class ClassEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long classSlotId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String status;   // ACTIVE | INACTIVE | PENDING

    private LocalDateTime enrolledAt;

    @PrePersist
    void prePersist() {
        this.enrolledAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClassSlotId() { return classSlotId; }
    public void setClassSlotId(Long classSlotId) { this.classSlotId = classSlotId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }
}
