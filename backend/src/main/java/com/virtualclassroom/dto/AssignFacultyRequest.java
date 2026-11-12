package com.virtualclassroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload to assign a faculty instructor to a subject")
public class AssignFacultyRequest {

    @NotNull(message = "Faculty identifier is required")
    @Schema(description = "User identifier of the faculty member", example = "42")
    private Long facultyId;

    @Schema(description = "Academic term for assignment", example = "2026-ACTIVE")
    private String academicTerm = "2026-ACTIVE";

    public AssignFacultyRequest() {}

    public AssignFacultyRequest(Long facultyId, String academicTerm) {
        this.facultyId = facultyId;
        this.academicTerm = academicTerm != null ? academicTerm : "2026-ACTIVE";
    }

    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }
    public String getAcademicTerm() { return academicTerm; }
    public void setAcademicTerm(String academicTerm) { this.academicTerm = academicTerm; }
}
