package com.fyp.fypsystem.model;

import jakarta.persistence.*;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long fromId;
    private Long toId;
    private String fromName;
    private String toName;
    @Column(columnDefinition = "TEXT")
    private String message;
    private String sentAt;
    private Boolean isRead;

    @PrePersist
    protected void prePersist() {
        if (sentAt == null) sentAt = java.time.LocalDateTime.now().toString();
        if (isRead == null) isRead = false;
    }

    public Long getId() { return id; }
    public Long getFromId() { return fromId; }
    public void setFromId(Long fromId) { this.fromId = fromId; }
    public Long getToId() { return toId; }
    public void setToId(Long toId) { this.toId = toId; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public String getToName() { return toName; }
    public void setToName(String toName) { this.toName = toName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSentAt() { return sentAt; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
}
