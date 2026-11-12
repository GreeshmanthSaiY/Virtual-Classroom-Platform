package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "assignment_submissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_assignment_student", columnNames = {"assignment_id", "student_id"})
})
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "submission_text", columnDefinition = "TEXT")
    private String submissionText;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false)
    private Instant submittedAt;

    @Column(name = "grade_points")
    private Integer gradePoints;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    public AssignmentSubmission() {}

    public AssignmentSubmission(Assignment assignment, User student, String submissionText, String fileUrl, SubmissionStatus status) {
        this.assignment = assignment;
        this.student = student;
        this.submissionText = submissionText;
        this.fileUrl = fileUrl;
        this.status = status != null ? status : SubmissionStatus.SUBMITTED;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Assignment assignment;
        private User student;
        private String submissionText;
        private String fileUrl;
        private Integer gradePoints;
        private String feedback;
        private User gradedBy;
        private Instant gradedAt;
        private Instant submittedAt;
        private SubmissionStatus status = SubmissionStatus.SUBMITTED;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder assignment(Assignment assignment) { this.assignment = assignment; return this; }
        public Builder student(User student) { this.student = student; return this; }
        public Builder submissionText(String submissionText) { this.submissionText = submissionText; return this; }
        public Builder fileUrl(String fileUrl) { this.fileUrl = fileUrl; return this; }
        public Builder gradePoints(Integer gradePoints) { this.gradePoints = gradePoints; return this; }
        public Builder feedback(String feedback) { this.feedback = feedback; return this; }
        public Builder gradedBy(User gradedBy) { this.gradedBy = gradedBy; return this; }
        public Builder gradedAt(Instant gradedAt) { this.gradedAt = gradedAt; return this; }
        public Builder submittedAt(Instant submittedAt) { this.submittedAt = submittedAt; return this; }
        public Builder status(SubmissionStatus status) { this.status = status; return this; }
        public AssignmentSubmission build() {
            AssignmentSubmission submission = new AssignmentSubmission(assignment, student, submissionText, fileUrl, status);
            submission.setId(id);
            submission.setGradePoints(gradePoints);
            submission.setFeedback(feedback);
            submission.setGradedBy(gradedBy);
            submission.setGradedAt(gradedAt);
            if (submittedAt != null) {
                submission.setSubmittedAt(submittedAt);
            }
            return submission;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Assignment getAssignment() { return assignment; }
    public void setAssignment(Assignment assignment) { this.assignment = assignment; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
    public String getSubmissionText() { return submissionText; }
    public void setSubmissionText(String submissionText) { this.submissionText = submissionText; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Integer getGradePoints() { return gradePoints; }
    public void setGradePoints(Integer gradePoints) { this.gradePoints = gradePoints; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public User getGradedBy() { return gradedBy; }
    public void setGradedBy(User gradedBy) { this.gradedBy = gradedBy; }
    public Instant getGradedAt() { return gradedAt; }
    public void setGradedAt(Instant gradedAt) { this.gradedAt = gradedAt; }
    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
}

