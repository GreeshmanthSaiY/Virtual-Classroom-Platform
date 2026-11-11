package com.virtualclassroom.controller;

import com.virtualclassroom.dto.*;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_ADMIN')")
@Tag(name = "Student Operations", description = "Student Classroom & Dashboard Endpoints")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }


    @GetMapping("/course")
    @Operation(summary = "Get student's enrolled degree and course details")
    public ResponseEntity<ApiResponse<CourseDto>> getEnrolledCourse(@AuthenticationPrincipal UserPrincipal principal) {
        CourseDto course = studentService.getEnrolledCourse(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(course, "Enrolled course retrieved"));
    }

    @GetMapping("/subjects")
    @Operation(summary = "Get subjects matching student's enrolled course and current year")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getEnrolledSubjects(@AuthenticationPrincipal UserPrincipal principal) {
        List<SubjectDto> subjects = studentService.getEnrolledSubjects(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(subjects, "Enrolled subjects retrieved"));
    }

    @GetMapping("/materials")
    @Operation(summary = "Get study materials for all enrolled subjects")
    public ResponseEntity<ApiResponse<List<StudyMaterialDto>>> getEnrolledMaterials(
            @RequestParam(required = false) Integer subjectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<StudyMaterialDto> materials = studentService.getAllEnrolledMaterials(principal.getId(), subjectId);
        return ResponseEntity.ok(ApiResponse.success(materials, "Study materials retrieved"));
    }

    @GetMapping("/lectures")
    @Operation(summary = "Get video lectures for all enrolled subjects")
    public ResponseEntity<ApiResponse<List<VideoLectureDto>>> getEnrolledLectures(
            @RequestParam(required = false) Integer subjectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<VideoLectureDto> lectures = studentService.getAllEnrolledLectures(principal.getId(), subjectId);
        return ResponseEntity.ok(ApiResponse.success(lectures, "Video lectures retrieved"));
    }

    @GetMapping("/subjects/{subjectId}/materials")
    @Operation(summary = "Get study materials for an enrolled subject")
    public ResponseEntity<ApiResponse<List<StudyMaterialDto>>> getSubjectMaterials(
            @PathVariable Integer subjectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (!"ROLE_ADMIN".equals(principal.getRole())) {
            studentService.verifyStudentEnrolledInSubject(principal.getId(), subjectId);
        }
        List<StudyMaterialDto> materials = studentService.getMaterialsForSubject(subjectId);
        return ResponseEntity.ok(ApiResponse.success(materials, "Study materials retrieved"));
    }

    @GetMapping("/subjects/{subjectId}/lectures")
    @Operation(summary = "Get video lectures for an enrolled subject")
    public ResponseEntity<ApiResponse<List<VideoLectureDto>>> getSubjectLectures(
            @PathVariable Integer subjectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (!"ROLE_ADMIN".equals(principal.getRole())) {
            studentService.verifyStudentEnrolledInSubject(principal.getId(), subjectId);
        }
        List<VideoLectureDto> lectures = studentService.getLecturesForSubject(subjectId);
        return ResponseEntity.ok(ApiResponse.success(lectures, "Video lectures retrieved"));
    }
}
