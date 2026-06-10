package com.fyp.fypsystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_slots")
public class ClassSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long coachId;
    private String coachName;
    private String name;           // e.g. "Ahmad-1234" or custom label
    private String dayOfWeek;      // MONDAY, TUESDAY, …
    private String startTime;      // "09:00"
    private String endTime;        // "10:00"
    private String classType;      // "Individual" | "Group"
    private String zoomAccount;
    private String zoomLink;
    private String meetingId;
    private String status;         // ACTIVE | INACTIVE
    private Integer monthlySessionCount;

    @Column(length = 500)
    private String sessionDates;   // free-text, e.g. "5, 12, 19 & 26 Jun 2026"

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCoachId() { return coachId; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }
    public String getCoachName() { return coachName; }
    public void setCoachName(String coachName) { this.coachName = coachName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getClassType() { return classType; }
    public void setClassType(String classType) { this.classType = classType; }
    public String getZoomAccount() { return zoomAccount; }
    public void setZoomAccount(String zoomAccount) { this.zoomAccount = zoomAccount; }
    public String getZoomLink() { return zoomLink; }
    public void setZoomLink(String zoomLink) { this.zoomLink = zoomLink; }
    public String getMeetingId() { return meetingId; }
    public void setMeetingId(String meetingId) { this.meetingId = meetingId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getMonthlySessionCount() { return monthlySessionCount; }
    public void setMonthlySessionCount(Integer monthlySessionCount) { this.monthlySessionCount = monthlySessionCount; }
    public String getSessionDates() { return sessionDates; }
    public void setSessionDates(String sessionDates) { this.sessionDates = sessionDates; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
