package com.virtualclassroom.controller;

import com.virtualclassroom.dto.ApiResponse;
import com.virtualclassroom.dto.CourseDto;
import com.virtualclassroom.dto.DegreeDto;
import com.virtualclassroom.dto.SubjectDto;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@Tag(name = "Curriculum & Programs", description = "Public & Academic Course Directory")
public class CourseController {

    private final CourseService courseService;
    private final UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public CourseController(CourseService courseService, UserRepository userRepository) {
        this.courseService = courseService;
        this.userRepository = userRepository;
    }

    public CourseController(CourseService courseService) {
        this(courseService, null);
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

    @PatchMapping("/{courseId}/archive")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Archive an academic course (Admin only)")
    public ResponseEntity<ApiResponse<CourseDto>> archiveCourse(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User admin = (userRepository != null && principal != null) ? userRepository.findById(principal.getId()).orElse(null) : null;
        CourseDto course = courseService.archiveCourse(courseId, admin, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(course, "Course archived successfully"));
    }

    @PatchMapping("/{courseId}/unarchive")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Unarchive an academic course (Admin only)")
    public ResponseEntity<ApiResponse<CourseDto>> unarchiveCourse(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User admin = (userRepository != null && principal != null) ? userRepository.findById(principal.getId()).orElse(null) : null;
        CourseDto course = courseService.unarchiveCourse(courseId, admin, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(course, "Course unarchived successfully"));
    }
}
