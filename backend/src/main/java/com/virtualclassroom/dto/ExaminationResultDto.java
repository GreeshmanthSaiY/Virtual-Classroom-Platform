package com.virtualclassroom.dto;

import com.virtualclassroom.model.ExaminationResult;

import java.math.BigDecimal;
import java.time.Instant;

public class ExaminationResultDto {

    private Long id;
    private Long examinationId;
    private String examinationTitle;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private BigDecimal marksObtained;
    private Integer totalMarks;
    private String grade;
    private String remarks;
    private String gradedByName;
    private Instant gradedAt;

    public ExaminationResultDto() {}

    public static ExaminationResultDto fromEntity(ExaminationResult result) {
        ExaminationResultDto dto = new ExaminationResultDto();
        dto.setId(result.getId());
        if (result.getExamination() != null) {
            dto.setExaminationId(result.getExamination().getId());
            dto.setExaminationTitle(result.getExamination().getTitle());
            dto.setTotalMarks(result.getExamination().getTotalMarks());
        }
        if (result.getStudent() != null) {
            dto.setStudentId(result.getStudent().getId());
            dto.setStudentName(result.getStudent().getFullName());
            dto.setStudentEmail(result.getStudent().getEmail());
        }
        dto.setMarksObtained(result.getMarksObtained());
        dto.setGrade(result.getGrade());
        dto.setRemarks(result.getRemarks());
        if (result.getGradedBy() != null) {
            dto.setGradedByName(result.getGradedBy().getFullName());
        }
        dto.setGradedAt(result.getGradedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getExaminationId() { return examinationId; }
    public void setExaminationId(Long examinationId) { this.examinationId = examinationId; }
    public String getExaminationTitle() { return examinationTitle; }
    public void setExaminationTitle(String examinationTitle) { this.examinationTitle = examinationTitle; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public BigDecimal getMarksObtained() { return marksObtained; }
    public void setMarksObtained(BigDecimal marksObtained) { this.marksObtained = marksObtained; }
    public Integer getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Integer totalMarks) { this.totalMarks = totalMarks; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getGradedByName() { return gradedByName; }
    public void setGradedByName(String gradedByName) { this.gradedByName = gradedByName; }
    public Instant getGradedAt() { return gradedAt; }
    public void setGradedAt(Instant gradedAt) { this.gradedAt = gradedAt; }
}
