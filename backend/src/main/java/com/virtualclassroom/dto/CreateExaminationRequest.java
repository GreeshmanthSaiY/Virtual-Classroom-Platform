package com.virtualclassroom.dto;

import com.virtualclassroom.model.ExamType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class CreateExaminationRequest {

    @NotNull(message = "Subject ID is required")
    private Integer subjectId;

    @NotBlank(message = "Examination title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    private ExamType examType = ExamType.MIDTERM;

    @NotNull(message = "Total marks are required")
    @Min(value = 1, message = "Total marks must be at least 1")
    private Integer totalMarks = 100;

    @NotNull(message = "Duration in minutes is required")
    @Min(value = 5, message = "Duration must be at least 5 minutes")
    private Integer durationMinutes = 60;

    @NotNull(message = "Scheduled time is required")
    private Instant scheduledAt;

    private String instructions;

    public CreateExaminationRequest() {}

    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
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
}
