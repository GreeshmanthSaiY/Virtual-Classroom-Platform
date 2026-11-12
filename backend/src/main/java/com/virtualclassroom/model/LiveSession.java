package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "live_sessions")
public class LiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "meeting_link", nullable = false, length = 500)
    private String meetingLink;

    @Column(nullable = false, length = 50)
    private String platform = "GOOGLE_MEET";

    @Column(name = "scheduled_start", nullable = false)
    private Instant scheduledStart;

    @Column(name = "scheduled_end", nullable = false)
    private Instant scheduledEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LiveSessionStatus status = LiveSessionStatus.SCHEDULED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public LiveSession() {}

    public LiveSession(Subject subject, User host, String title, String description, String meetingLink,
                       String platform, Instant scheduledStart, Instant scheduledEnd, LiveSessionStatus status) {
        this.subject = subject;
        this.host = host;
        this.title = title;
        this.description = description;
        this.meetingLink = meetingLink;
        this.platform = platform != null ? platform : "GOOGLE_MEET";
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.status = status != null ? status : LiveSessionStatus.SCHEDULED;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Subject subject;
        private User host;
        private String title;
        private String description;
        private String meetingLink;
        private String platform = "GOOGLE_MEET";
        private Instant scheduledStart;
        private Instant scheduledEnd;
        private LiveSessionStatus status = LiveSessionStatus.SCHEDULED;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder subject(Subject subject) { this.subject = subject; return this; }
        public Builder host(User host) { this.host = host; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder meetingLink(String meetingLink) { this.meetingLink = meetingLink; return this; }
        public Builder platform(String platform) { this.platform = platform; return this; }
        public Builder scheduledStart(Instant scheduledStart) { this.scheduledStart = scheduledStart; return this; }
        public Builder scheduledEnd(Instant scheduledEnd) { this.scheduledEnd = scheduledEnd; return this; }
        public Builder status(LiveSessionStatus status) { this.status = status; return this; }
        public LiveSession build() {
            LiveSession session = new LiveSession(subject, host, title, description, meetingLink, platform, scheduledStart, scheduledEnd, status);
            session.setId(id);
            return session;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public User getHost() { return host; }
    public void setHost(User host) { this.host = host; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public Instant getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(Instant scheduledStart) { this.scheduledStart = scheduledStart; }
    public Instant getScheduledEnd() { return scheduledEnd; }
    public void setScheduledEnd(Instant scheduledEnd) { this.scheduledEnd = scheduledEnd; }
    public LiveSessionStatus getStatus() { return status; }
    public void setStatus(LiveSessionStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
