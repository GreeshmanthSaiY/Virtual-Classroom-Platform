package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "video_lectures")
public class VideoLecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private User faculty;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "video_url", nullable = false, length = 500)
    private String videoUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public VideoLecture() {}

    public VideoLecture(Long id, Subject subject, User faculty, String title, String description, String videoUrl, Integer durationSeconds) {
        this.id = id;
        this.subject = subject;
        this.faculty = faculty;
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.durationSeconds = durationSeconds != null ? durationSeconds : 0;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Subject subject;
        private User faculty;
        private String title;
        private String description;
        private String videoUrl;
        private Integer durationSeconds = 0;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder subject(Subject subject) { this.subject = subject; return this; }
        public Builder faculty(User faculty) { this.faculty = faculty; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder videoUrl(String videoUrl) { this.videoUrl = videoUrl; return this; }
        public Builder durationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; return this; }
        public VideoLecture build() {
            return new VideoLecture(id, subject, faculty, title, description, videoUrl, durationSeconds);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public User getFaculty() { return faculty; }
    public void setFaculty(User faculty) { this.faculty = faculty; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
