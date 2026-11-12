package com.virtualclassroom.dto;

import com.virtualclassroom.model.AssignmentSubmission;
import com.virtualclassroom.model.SubmissionStatus;

import java.time.Instant;

public class AssignmentSubmissionDto {

    private Long id;
    private Long assignmentId;
    private String assignmentTitle;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String submissionText;
    private String fileUrl;
    private Instant submittedAt;
    private Integer gradePoints;
    private Integer maxPoints;
    private String feedback;
    private String gradedByName;
    private Instant gradedAt;
    private SubmissionStatus status;

    public AssignmentSubmissionDto() {}

    public static AssignmentSubmissionDto fromEntity(AssignmentSubmission submission) {
        AssignmentSubmissionDto dto = new AssignmentSubmissionDto();
        dto.setId(submission.getId());
        if (submission.getAssignment() != null) {
            dto.setAssignmentId(submission.getAssignment().getId());
            dto.setAssignmentTitle(submission.getAssignment().getTitle());
            dto.setMaxPoints(submission.getAssignment().getMaxPoints());
        }
        if (submission.getStudent() != null) {
            dto.setStudentId(submission.getStudent().getId());
            dto.setStudentName(submission.getStudent().getFullName());
            dto.setStudentEmail(submission.getStudent().getEmail());
        }
        dto.setSubmissionText(submission.getSubmissionText());
        dto.setFileUrl(submission.getFileUrl());
        dto.setSubmittedAt(submission.getSubmittedAt());
        dto.setGradePoints(submission.getGradePoints());
        dto.setFeedback(submission.getFeedback());
        if (submission.getGradedBy() != null) {
            dto.setGradedByName(submission.getGradedBy().getFullName());
        }
        dto.setGradedAt(submission.getGradedAt());
        dto.setStatus(submission.getStatus());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    public String getAssignmentTitle() { return assignmentTitle; }
    public void setAssignmentTitle(String assignmentTitle) { this.assignmentTitle = assignmentTitle; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public String getSubmissionText() { return submissionText; }
    public void setSubmissionText(String submissionText) { this.submissionText = submissionText; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Integer getGradePoints() { return gradePoints; }
    public void setGradePoints(Integer gradePoints) { this.gradePoints = gradePoints; }
    public Integer getMaxPoints() { return maxPoints; }
    public void setMaxPoints(Integer maxPoints) { this.maxPoints = maxPoints; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public String getGradedByName() { return gradedByName; }
    public void setGradedByName(String gradedByName) { this.gradedByName = gradedByName; }
    public Instant getGradedAt() { return gradedAt; }
    public void setGradedAt(Instant gradedAt) { this.gradedAt = gradedAt; }
    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
}

