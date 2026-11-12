package com.virtualclassroom.dto;

import com.virtualclassroom.model.ExamType;
import com.virtualclassroom.model.Examination;

import java.time.Instant;

public class ExaminationDto {

    private Long id;
    private Integer subjectId;
    private String subjectTitle;
    private Long creatorId;
    private String creatorName;
    private String title;
    private ExamType examType;
    private Integer totalMarks;
    private Integer durationMinutes;
    private Instant scheduledAt;
    private String instructions;
    private Instant createdAt;

    public ExaminationDto() {}

    public static ExaminationDto fromEntity(Examination exam) {
        ExaminationDto dto = new ExaminationDto();
        dto.setId(exam.getId());
        if (exam.getSubject() != null) {
            dto.setSubjectId(exam.getSubject().getId());
            dto.setSubjectTitle(exam.getSubject().getTitle());
        }
        if (exam.getCreator() != null) {
            dto.setCreatorId(exam.getCreator().getId());
            dto.setCreatorName(exam.getCreator().getFullName());
        }
        dto.setTitle(exam.getTitle());
        dto.setExamType(exam.getExamType());
        dto.setTotalMarks(exam.getTotalMarks());
        dto.setDurationMinutes(exam.getDurationMinutes());
        dto.setScheduledAt(exam.getScheduledAt());
        dto.setInstructions(exam.getInstructions());
        dto.setCreatedAt(exam.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public String getSubjectTitle() { return subjectTitle; }
    public void setSubjectTitle(String subjectTitle) { this.subjectTitle = subjectTitle; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public ExamType getExamType() { return examType; }
    public void setExamType(ExamType examType) { this.examType = examType; }
    public Integer getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Integer totalMarks) { this.totalMarks = totalMarks; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

