package com.fyp.fypsystem.model;

import jakarta.persistence.*;

@Entity
@Table(name = "session_plans")
public class SessionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long coachId;
    private Long studentId;
    private String studentName;
    private String coachName;
    private Integer sessionsPerMonth;
    private Double pricePerSession;
    private String month;
    private String status;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private String zoomLink;
    private String classDate;
    private String classTime;
    @Column(columnDefinition = "TEXT")
    private String coachNote;
    private String createdAt;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = java.time.LocalDateTime.now().toString();
        if (status == null) status = "PENDING_PAYMENT";
        if (sessionsPerMonth == null) sessionsPerMonth = 4;
    }

    public Long getId() { return id; }
    public Long getCoachId() { return coachId; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getCoachName() { return coachName; }
    public void setCoachName(String coachName) { this.coachName = coachName; }
    public Integer getSessionsPerMonth() { return sessionsPerMonth; }
    public void setSessionsPerMonth(Integer sessionsPerMonth) { this.sessionsPerMonth = sessionsPerMonth; }
    public Double getPricePerSession() { return pricePerSession; }
    public void setPricePerSession(Double pricePerSession) { this.pricePerSession = pricePerSession; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getZoomLink() { return zoomLink; }
    public void setZoomLink(String zoomLink) { this.zoomLink = zoomLink; }
    public String getClassDate() { return classDate; }
    public void setClassDate(String classDate) { this.classDate = classDate; }
    public String getClassTime() { return classTime; }
    public void setClassTime(String classTime) { this.classTime = classTime; }
    public String getCoachNote() { return coachNote; }
    public void setCoachNote(String coachNote) { this.coachNote = coachNote; }
    public String getCreatedAt() { return createdAt; }
}
