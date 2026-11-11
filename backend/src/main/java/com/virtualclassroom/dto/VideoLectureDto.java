package com.virtualclassroom.dto;


import java.time.Instant;

public class VideoLectureDto {

    private Long id;
    private Integer subjectId;
    private String subjectTitle;
    private Long facultyId;
    private String facultyName;
    private String title;
    private String description;
    private String videoUrl;
    private Integer durationSeconds;
    private Instant createdAt;

    public VideoLectureDto() {}

    public VideoLectureDto(Long id, Integer subjectId, String subjectTitle, Long facultyId, String facultyName, String title, String description, String videoUrl, Integer durationSeconds, Instant createdAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.subjectTitle = subjectTitle;
        this.facultyId = facultyId;
        this.facultyName = facultyName;
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.durationSeconds = durationSeconds;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public String getSubjectTitle() { return subjectTitle; }
    public void setSubjectTitle(String subjectTitle) { this.subjectTitle = subjectTitle; }
    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }
    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Integer subjectId;
        private String subjectTitle;
        private Long facultyId;
        private String facultyName;
        private String title;
        private String description;
        private String videoUrl;
        private Integer durationSeconds;
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
        public Builder facultyId(Long facultyId) {
            this.facultyId = facultyId;
            return this;
        }
        public Builder facultyName(String facultyName) {
            this.facultyName = facultyName;
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
        public Builder videoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
            return this;
        }
        public Builder durationSeconds(Integer durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public VideoLectureDto build() {
            return new VideoLectureDto(id, subjectId, subjectTitle, facultyId, facultyName, title, description, videoUrl, durationSeconds, createdAt);
        }
    }
}
