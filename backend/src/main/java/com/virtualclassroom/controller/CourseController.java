package com.virtualclassroom.controller;

import com.virtualclassroom.dto.ApiResponse;
import com.virtualclassroom.dto.CourseDto;
import com.virtualclassroom.dto.DegreeDto;
import com.virtualclassroom.dto.SubjectDto;
import com.virtualclassroom.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@Tag(name = "Curriculum & Programs", description = "Public & Academic Course Directory")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }


    @GetMapping("/degrees")
    @Operation(summary = "Get all available academic degrees and associated programs")
    public ResponseEntity<ApiResponse<List<DegreeDto>>> getAllDegrees() {
        List<DegreeDto> degrees = courseService.getAllDegrees();
        return ResponseEntity.ok(ApiResponse.success(degrees, "Degrees retrieved successfully"));
    }

    @GetMapping("/by-degree/{degreeId}")
    @Operation(summary = "Get courses offered under a specific degree")
    public ResponseEntity<ApiResponse<List<CourseDto>>> getCoursesByDegree(@PathVariable Integer degreeId) {
        List<CourseDto> courses = courseService.getCoursesByDegree(degreeId);
        return ResponseEntity.ok(ApiResponse.success(courses, "Courses retrieved successfully"));
    }

    @GetMapping("/{courseId}/subjects")
    @Operation(summary = "Get subjects for a course optionally filtered by year level")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getSubjects(
            @PathVariable Integer courseId,
            @RequestParam(required = false) Integer year
    ) {
        List<SubjectDto> subjects = courseService.getSubjectsByCourseAndYear(courseId, year);
        return ResponseEntity.ok(ApiResponse.success(subjects, "Subjects retrieved successfully"));
    }

    @GetMapping("/subjects")
    @Operation(summary = "Get all system subjects")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getAllSubjects() {
        List<SubjectDto> subjects = courseService.getAllSubjects();
        return ResponseEntity.ok(ApiResponse.success(subjects, "All subjects retrieved successfully"));
    }
}
