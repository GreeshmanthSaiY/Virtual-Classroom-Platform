package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "study_materials")
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes = 0L;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType = "application/pdf";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public StudyMaterial() {}

    public StudyMaterial(Long id, Subject subject, User uploader, String title, String description, String fileUrl, Long fileSizeBytes, String mimeType) {
        this.id = id;
        this.subject = subject;
        this.uploader = uploader;
        this.title = title;
        this.description = description;
        this.fileUrl = fileUrl;
        this.fileSizeBytes = fileSizeBytes != null ? fileSizeBytes : 0L;
        this.mimeType = mimeType != null ? mimeType : "application/pdf";
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Subject subject;
        private User uploader;
        private String title;
        private String description;
        private String fileUrl;
        private Long fileSizeBytes = 0L;
        private String mimeType = "application/pdf";

        public Builder id(Long id) { this.id = id; return this; }
        public Builder subject(Subject subject) { this.subject = subject; return this; }
        public Builder uploader(User uploader) { this.uploader = uploader; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder fileUrl(String fileUrl) { this.fileUrl = fileUrl; return this; }
        public Builder fileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public StudyMaterial build() {
            return new StudyMaterial(id, subject, uploader, title, description, fileUrl, fileSizeBytes, mimeType);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public User getUploader() { return uploader; }
    public void setUploader(User uploader) { this.uploader = uploader; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
