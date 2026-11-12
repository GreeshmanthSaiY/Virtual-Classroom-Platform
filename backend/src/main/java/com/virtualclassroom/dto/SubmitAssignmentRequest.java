package com.virtualclassroom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SubmitAssignmentRequest {

    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;

    private String submissionText;

    @Size(max = 500, message = "File URL cannot exceed 500 characters")
    private String fileUrl;

    public SubmitAssignmentRequest() {}

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    public String getSubmissionText() { return submissionText; }
    public void setSubmissionText(String submissionText) { this.submissionText = submissionText; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
}

