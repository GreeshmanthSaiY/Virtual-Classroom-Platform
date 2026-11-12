package com.virtualclassroom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class GradeSubmissionRequest {

    @NotNull(message = "Grade points are required")
    @Min(value = 0, message = "Grade points cannot be negative")
    private Integer gradePoints;

    private String feedback;

    public GradeSubmissionRequest() {}

    public Integer getGradePoints() { return gradePoints; }
    public void setGradePoints(Integer gradePoints) { this.gradePoints = gradePoints; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}

