package com.virtualclassroom.dto;


import java.time.Instant;

public class StudyMaterialDto {

    private Long id;
    private Integer subjectId;
    private String subjectTitle;
    private Long uploaderId;
    private String uploaderName;
    private String title;
    private String description;
    private String fileUrl;
    private Long fileSizeBytes;
    private String mimeType;
    private Instant createdAt;

    public StudyMaterialDto() {}

    public StudyMaterialDto(Long id, Integer subjectId, String subjectTitle, Long uploaderId, String uploaderName, String title, String description, String fileUrl, Long fileSizeBytes, String mimeType, Instant createdAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.subjectTitle = subjectTitle;
        this.uploaderId = uploaderId;
        this.uploaderName = uploaderName;
        this.title = title;
        this.description = description;
        this.fileUrl = fileUrl;
        this.fileSizeBytes = fileSizeBytes;
        this.mimeType = mimeType;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public String getSubjectTitle() { return subjectTitle; }
    public void setSubjectTitle(String subjectTitle) { this.subjectTitle = subjectTitle; }
    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }
    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Integer subjectId;
        private String subjectTitle;
        private Long uploaderId;
        private String uploaderName;
        private String title;
        private String description;
        private String fileUrl;
        private Long fileSizeBytes;
        private String mimeType;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        public Builder subjectId(Integer subjectId) {
            this.subjectId = subjectId;
            return this;
        }
        public Builder subjectTitle(String subjectTitle) {
            this.subjectTitle = subjectTitle;
            return this;
        }
        public Builder uploaderId(Long uploaderId) {
            this.uploaderId = uploaderId;
            return this;
        }
        public Builder uploaderName(String uploaderName) {
            this.uploaderName = uploaderName;
            return this;
        }
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        public Builder fileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
            return this;
        }
        public Builder fileSizeBytes(Long fileSizeBytes) {
            this.fileSizeBytes = fileSizeBytes;
            return this;
        }
        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public StudyMaterialDto build() {
            return new StudyMaterialDto(id, subjectId, subjectTitle, uploaderId, uploaderName, title, description, fileUrl, fileSizeBytes, mimeType, createdAt);
        }
    }
}
