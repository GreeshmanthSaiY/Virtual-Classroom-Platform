package com.virtualclassroom.dto;

import com.virtualclassroom.model.LiveSession;
import com.virtualclassroom.model.LiveSessionStatus;

import java.time.Instant;

public class LiveSessionDto {

    private Long id;
    private Integer subjectId;
    private String subjectTitle;
    private Long hostId;
    private String hostName;
    private String title;
    private String description;
    private String meetingLink;
    private String platform;
    private Instant scheduledStart;
    private Instant scheduledEnd;
    private LiveSessionStatus status;
    private Instant createdAt;

    public LiveSessionDto() {}

    public static LiveSessionDto fromEntity(LiveSession session) {
        LiveSessionDto dto = new LiveSessionDto();
        dto.setId(session.getId());
        if (session.getSubject() != null) {
            dto.setSubjectId(session.getSubject().getId());
            dto.setSubjectTitle(session.getSubject().getTitle());
        }
        if (session.getHost() != null) {
            dto.setHostId(session.getHost().getId());
            dto.setHostName(session.getHost().getFullName());
        }
        dto.setTitle(session.getTitle());
        dto.setDescription(session.getDescription());
        dto.setMeetingLink(session.getMeetingLink());
        dto.setPlatform(session.getPlatform());
        dto.setScheduledStart(session.getScheduledStart());
        dto.setScheduledEnd(session.getScheduledEnd());
        dto.setStatus(session.getStatus());
        dto.setCreatedAt(session.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public String getSubjectTitle() { return subjectTitle; }
    public void setSubjectTitle(String subjectTitle) { this.subjectTitle = subjectTitle; }
    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
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

