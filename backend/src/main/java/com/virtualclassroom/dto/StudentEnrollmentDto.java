package com.virtualclassroom.dto;

import com.virtualclassroom.model.EnrollmentStatus;
import com.virtualclassroom.model.StudentSubjectEnrollment;

import java.time.Instant;

public class StudentEnrollmentDto {

    private Long id;
    private Long studentId;
    private String studentName;
    private Integer subjectId;
    private String subjectCode;
    private String subjectTitle;
    private Integer semester;
    private Integer credits;
    private String academicTerm;
    private EnrollmentStatus status;
    private Instant enrolledAt;

    public StudentEnrollmentDto() {}

    public static StudentEnrollmentDto fromEntity(StudentSubjectEnrollment enrollment) {
        StudentEnrollmentDto dto = new StudentEnrollmentDto();
        dto.setId(enrollment.getId());
        if (enrollment.getStudent() != null) {
            dto.setStudentId(enrollment.getStudent().getId());
            dto.setStudentName(enrollment.getStudent().getFullName());
        }
        if (enrollment.getSubject() != null) {
            dto.setSubjectId(enrollment.getSubject().getId());
            dto.setSubjectCode(enrollment.getSubject().getCode());
            dto.setSubjectTitle(enrollment.getSubject().getTitle());
            dto.setSemester(enrollment.getSubject().getSemester());
            dto.setCredits(enrollment.getSubject().getCredits());
        }
        dto.setAcademicTerm(enrollment.getAcademicTerm());
        dto.setStatus(enrollment.getStatus());
        dto.setEnrolledAt(enrollment.getEnrolledAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getSubjectTitle() { return subjectTitle; }
    public void setSubjectTitle(String subjectTitle) { this.subjectTitle = subjectTitle; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
    public String getAcademicTerm() { return academicTerm; }
    public void setAcademicTerm(String academicTerm) { this.academicTerm = academicTerm; }
    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }
    public Instant getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(Instant enrolledAt) { this.enrolledAt = enrolledAt; }
}
