package com.virtualclassroom.controller;

import com.virtualclassroom.dto.ApiResponse;
import com.virtualclassroom.dto.StudentEnrollmentDto;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
@Tag(name = "Enrollments", description = "Student Academic Subject Enrollment Endpoints")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final UserRepository userRepository;

    public EnrollmentController(EnrollmentService enrollmentService, UserRepository userRepository) {
        this.enrollmentService = enrollmentService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_ADMIN')")
    @Operation(summary = "Enroll current student in an academic subject")
    public ResponseEntity<ApiResponse<StudentEnrollmentDto>> enroll(
            @RequestParam Integer subjectId,
            @RequestParam(required = false, defaultValue = "2026-ACTIVE") String term,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String clientIp = servletRequest.getRemoteAddr();
        StudentEnrollmentDto enrollment = enrollmentService.enrollStudentInSubject(principal.getId(), subjectId, term, user, clientIp);
        return ResponseEntity.ok(ApiResponse.success(enrollment, "Successfully enrolled in subject"));
    }

    @GetMapping({"/my-courses", "/my-subjects"})
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_ADMIN')")
    @Operation(summary = "Get list of subjects currently enrolled by the student")
    public ResponseEntity<ApiResponse<List<StudentEnrollmentDto>>> getMyEnrollments(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<StudentEnrollmentDto> enrollments = enrollmentService.getStudentEnrollments(principal.getId(), user);
        return ResponseEntity.ok(ApiResponse.success(enrollments, "Enrollments retrieved successfully"));
    }

    @GetMapping("/subject/{subjectId}")
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    @Operation(summary = "Get all students enrolled in a subject")
    public ResponseEntity<ApiResponse<List<StudentEnrollmentDto>>> getSubjectEnrollments(
            @PathVariable Integer subjectId
    ) {
        List<StudentEnrollmentDto> enrollments = enrollmentService.getSubjectEnrollments(subjectId);
        return ResponseEntity.ok(ApiResponse.success(enrollments, "Subject enrollments retrieved successfully"));
    }
}

