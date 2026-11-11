package com.virtualclassroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateLectureRequest {

    @NotNull(message = "Subject ID is required")
    private Integer subjectId;
    @NotBlank(message = "Title is required")
    private String title;
    private String description;
    @NotBlank(message = "Video URL is required")
    private String videoUrl;
    private Integer durationSeconds;

    public CreateLectureRequest() {}

    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
}
