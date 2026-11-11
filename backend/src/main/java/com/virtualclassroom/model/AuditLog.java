package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_entity", nullable = false, length = 50)
    private String targetEntity;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private Instant timestamp;

    public AuditLog() {
    }

    public AuditLog(Long id, User actor, String actorEmail, String action, String targetEntity, String targetId, String details, String ipAddress, Instant timestamp) {
        this.id = id;
        this.actor = actor;
        this.actorEmail = actorEmail;
        this.action = action;
        this.targetEntity = targetEntity;
        this.targetId = targetId;
        this.details = details;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }
    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetEntity() { return targetEntity; }
    public void setTargetEntity(String targetEntity) { this.targetEntity = targetEntity; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public static class Builder {
        private Long id;
        private User actor;
        private String actorEmail;
        private String action;
        private String targetEntity;
        private String targetId;
        private String details;
        private String ipAddress;
        private Instant timestamp;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder actor(User actor) { this.actor = actor; return this; }
        public Builder actorEmail(String actorEmail) { this.actorEmail = actorEmail; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder targetEntity(String targetEntity) { this.targetEntity = targetEntity; return this; }
        public Builder targetId(String targetId) { this.targetId = targetId; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public AuditLog build() {
            return new AuditLog(id, actor, actorEmail, action, targetEntity, targetId, details, ipAddress, timestamp);
        }
    }
}
