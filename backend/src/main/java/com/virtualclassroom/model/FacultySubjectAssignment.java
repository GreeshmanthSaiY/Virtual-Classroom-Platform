package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "faculty_subject_assignments")
@IdClass(FacultySubjectAssignment.AssignmentId.class)
public class FacultySubjectAssignment {

    @Id
    @Column(name = "faculty_id")
    private Long facultyId;

    @Id
    @Column(name = "subject_id")
    private Integer subjectId;

    @Id
    @Column(name = "academic_term", length = 20)
    private String academicTerm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", insertable = false, updatable = false)
    private FacultyProfile facultyProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false)
    private Instant assignedAt;

    public FacultySubjectAssignment() {}

    public FacultySubjectAssignment(Long facultyId, Integer subjectId, String academicTerm, FacultyProfile facultyProfile, Subject subject) {
        this.facultyId = facultyId;
        this.subjectId = subjectId;
        this.academicTerm = academicTerm;
        this.facultyProfile = facultyProfile;
        this.subject = subject;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long facultyId;
        private Integer subjectId;
        private String academicTerm;
        private FacultyProfile facultyProfile;
        private Subject subject;

        public Builder facultyId(Long facultyId) { this.facultyId = facultyId; return this; }
        public Builder subjectId(Integer subjectId) { this.subjectId = subjectId; return this; }
        public Builder academicTerm(String academicTerm) { this.academicTerm = academicTerm; return this; }
        public Builder facultyProfile(FacultyProfile facultyProfile) { this.facultyProfile = facultyProfile; return this; }
        public Builder subject(Subject subject) { this.subject = subject; return this; }
        public FacultySubjectAssignment build() {
            return new FacultySubjectAssignment(facultyId, subjectId, academicTerm, facultyProfile, subject);
        }
    }

    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public String getAcademicTerm() { return academicTerm; }
    public void setAcademicTerm(String academicTerm) { this.academicTerm = academicTerm; }
    public FacultyProfile getFacultyProfile() { return facultyProfile; }
    public void setFacultyProfile(FacultyProfile facultyProfile) { this.facultyProfile = facultyProfile; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public static class AssignmentId implements Serializable {
        private Long facultyId;
        private Integer subjectId;
        private String academicTerm;

        public AssignmentId() {}
        public AssignmentId(Long facultyId, Integer subjectId, String academicTerm) {
            this.facultyId = facultyId;
            this.subjectId = subjectId;
            this.academicTerm = academicTerm;
        }

        public Long getFacultyId() { return facultyId; }
        public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }
        public Integer getSubjectId() { return subjectId; }
        public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
        public String getAcademicTerm() { return academicTerm; }
        public void setAcademicTerm(String academicTerm) { this.academicTerm = academicTerm; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AssignmentId that = (AssignmentId) o;
            return Objects.equals(facultyId, that.facultyId) &&
                    Objects.equals(subjectId, that.subjectId) &&
                    Objects.equals(academicTerm, that.academicTerm);
        }

        @Override
        public int hashCode() {
            return Objects.hash(facultyId, subjectId, academicTerm);
        }
    }
}
