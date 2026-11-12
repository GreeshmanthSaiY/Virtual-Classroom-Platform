package com.virtualclassroom.dto;

import com.virtualclassroom.model.Assignment;

import java.time.Instant;

public class AssignmentDto {

    private Long id;
    private Integer subjectId;
    private String subjectTitle;
    private Long facultyId;
    private String facultyName;
    private String title;
    private String description;
    private Instant dueDate;
    private Integer maxPoints;
    private String attachmentUrl;
    private int submissionCount;
    private Instant createdAt;

    public AssignmentDto() {}

    public static AssignmentDto fromEntity(Assignment assignment) {
        AssignmentDto dto = new AssignmentDto();
        dto.setId(assignment.getId());
        if (assignment.getSubject() != null) {
            dto.setSubjectId(assignment.getSubject().getId());
            dto.setSubjectTitle(assignment.getSubject().getTitle());
        }
        if (assignment.getFaculty() != null) {
            dto.setFacultyId(assignment.getFaculty().getId());
            dto.setFacultyName(assignment.getFaculty().getFullName());
        }
        dto.setTitle(assignment.getTitle());
        dto.setDescription(assignment.getDescription());
        dto.setDueDate(assignment.getDueDate());
        dto.setMaxPoints(assignment.getMaxPoints());
        dto.setAttachmentUrl(assignment.getAttachmentUrl());
        dto.setSubmissionCount(assignment.getSubmissions() != null ? assignment.getSubmissions().size() : 0);
        dto.setCreatedAt(assignment.getCreatedAt());
        return dto;
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
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }
    public Integer getMaxPoints() { return maxPoints; }
    public void setMaxPoints(Integer maxPoints) { this.maxPoints = maxPoints; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    public int getSubmissionCount() { return submissionCount; }
    public void setSubmissionCount(int submissionCount) { this.submissionCount = submissionCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
