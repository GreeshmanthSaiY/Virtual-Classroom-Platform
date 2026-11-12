package com.virtualclassroom.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class CreateLiveSessionRequest {

    @NotNull(message = "Subject ID is required")
    private Integer subjectId;

    @NotBlank(message = "Session title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    private String description;

    @NotBlank(message = "Meeting link is required")
    @Size(max = 500, message = "Meeting link cannot exceed 500 characters")
    private String meetingLink;

    private String platform = "GOOGLE_MEET";

    @NotNull(message = "Scheduled start time is required")
    private Instant scheduledStart;

    @NotNull(message = "Scheduled end time is required")
    private Instant scheduledEnd;

    public CreateLiveSessionRequest() {}

    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
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
}
