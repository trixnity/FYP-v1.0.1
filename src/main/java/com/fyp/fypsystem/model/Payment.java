package com.fyp.fypsystem.model;

import jakarta.persistence.*;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long studentId;
    private Long coachId;
    private Long sessionPlanId;
    private String studentName;
    private String month;
    private Integer sessionCount;
    private Double totalAmount;
    private String status;
    private String paidAt;
    private String createdAt;
    private String stripeCheckoutSessionId;
    private String stripePaymentIntentId;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = java.time.LocalDateTime.now().toString();
        if (status == null) status = "PENDING";
    }

    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCoachId() { return coachId; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }
    public Long getSessionPlanId() { return sessionPlanId; }
    public void setSessionPlanId(Long sessionPlanId) { this.sessionPlanId = sessionPlanId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public Integer getSessionCount() { return sessionCount; }
    public void setSessionCount(Integer sessionCount) { this.sessionCount = sessionCount; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaidAt() { return paidAt; }
    public void setPaidAt(String paidAt) { this.paidAt = paidAt; }
    public String getCreatedAt() { return createdAt; }
    public String getStripeCheckoutSessionId() { return stripeCheckoutSessionId; }
    public void setStripeCheckoutSessionId(String stripeCheckoutSessionId) { this.stripeCheckoutSessionId = stripeCheckoutSessionId; }
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }
}
