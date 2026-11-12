package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "examination_results", uniqueConstraints = {
        @UniqueConstraint(name = "uk_exam_student", columnNames = {"examination_id", "student_id"})
})
public class ExaminationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examination_id", nullable = false)
    private Examination examination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "marks_obtained", nullable = false, precision = 5, scale = 2)
    private BigDecimal marksObtained;

    @Column(nullable = false, length = 10)
    private String grade;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    @CreationTimestamp
    @Column(name = "graded_at", updatable = false)
    private Instant gradedAt;

    public ExaminationResult() {}

    public ExaminationResult(Examination examination, User student, BigDecimal marksObtained, String grade, String remarks, User gradedBy) {
        this.examination = examination;
        this.student = student;
        this.marksObtained = marksObtained;
        this.grade = grade;
        this.remarks = remarks;
        this.gradedBy = gradedBy;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Examination examination;
        private User student;
        private BigDecimal marksObtained;
        private String grade;
        private String remarks;
        private User gradedBy;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder examination(Examination examination) { this.examination = examination; return this; }
        public Builder student(User student) { this.student = student; return this; }
        public Builder marksObtained(BigDecimal marksObtained) { this.marksObtained = marksObtained; return this; }
        public Builder grade(String grade) { this.grade = grade; return this; }
        public Builder remarks(String remarks) { this.remarks = remarks; return this; }
        public Builder gradedBy(User gradedBy) { this.gradedBy = gradedBy; return this; }
        public ExaminationResult build() {
            ExaminationResult result = new ExaminationResult(examination, student, marksObtained, grade, remarks, gradedBy);
            result.setId(id);
            return result;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Examination getExamination() { return examination; }
    public void setExamination(Examination examination) { this.examination = examination; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
    public BigDecimal getMarksObtained() { return marksObtained; }
    public void setMarksObtained(BigDecimal marksObtained) { this.marksObtained = marksObtained; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public User getGradedBy() { return gradedBy; }
    public void setGradedBy(User gradedBy) { this.gradedBy = gradedBy; }
    public Instant getGradedAt() { return gradedAt; }
    public void setGradedAt(Instant gradedAt) { this.gradedAt = gradedAt; }
}
