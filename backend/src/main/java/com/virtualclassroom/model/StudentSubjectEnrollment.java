package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "student_subject_enrollments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_subject_term", columnNames = {"student_id", "subject_id", "academic_term"})
})
public class StudentSubjectEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "academic_term", nullable = false, length = 30)
    private String academicTerm = "2026-ACTIVE";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

    @CreationTimestamp
    @Column(name = "enrolled_at", updatable = false)
    private Instant enrolledAt;

    public StudentSubjectEnrollment() {}

    public StudentSubjectEnrollment(User student, Subject subject, String academicTerm, EnrollmentStatus status) {
        this.student = student;
        this.subject = subject;
        this.academicTerm = academicTerm != null ? academicTerm : "2026-ACTIVE";
        this.status = status != null ? status : EnrollmentStatus.ENROLLED;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private User student;
        private Subject subject;
        private String academicTerm = "2026-ACTIVE";
        private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder student(User student) { this.student = student; return this; }
        public Builder subject(Subject subject) { this.subject = subject; return this; }
        public Builder academicTerm(String academicTerm) { this.academicTerm = academicTerm; return this; }
        public Builder status(EnrollmentStatus status) { this.status = status; return this; }
        public StudentSubjectEnrollment build() {
            StudentSubjectEnrollment enrollment = new StudentSubjectEnrollment(student, subject, academicTerm, status);
            enrollment.setId(id);
            return enrollment;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public String getAcademicTerm() { return academicTerm; }
    public void setAcademicTerm(String academicTerm) { this.academicTerm = academicTerm; }
    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }
    public Instant getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(Instant enrolledAt) { this.enrolledAt = enrolledAt; }
}

