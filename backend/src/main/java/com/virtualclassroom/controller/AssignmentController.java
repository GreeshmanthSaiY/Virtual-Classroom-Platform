package com.virtualclassroom.controller;

import com.virtualclassroom.dto.ApiResponse;
import com.virtualclassroom.dto.AssignmentDto;
import com.virtualclassroom.dto.AssignmentSubmissionDto;
import com.virtualclassroom.dto.CreateAssignmentRequest;
import com.virtualclassroom.dto.GradeSubmissionRequest;
import com.virtualclassroom.dto.SubmitAssignmentRequest;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assignments")
@Tag(name = "Assignments", description = "Coursework & Assignment Management Endpoints")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final UserRepository userRepository;

    public AssignmentController(AssignmentService assignmentService, UserRepository userRepository) {
        this.assignmentService = assignmentService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    @Operation(summary = "Create an assignment for an assigned subject")
    public ResponseEntity<ApiResponse<AssignmentDto>> createAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAssignmentRequest request,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String clientIp = servletRequest.getRemoteAddr();
        AssignmentDto created = assignmentService.createAssignment(request, user, clientIp);
        return ResponseEntity.ok(ApiResponse.success(created, "Assignment published successfully"));
    }

    @GetMapping("/subject/{subjectId}")
    @Operation(summary = "Get assignments for an academic subject (paged)")
    public ResponseEntity<ApiResponse<Page<AssignmentDto>>> getAssignmentsForSubject(
            @PathVariable Integer subjectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Page<AssignmentDto> assignments = assignmentService.getAssignmentsForSubjectPaged(subjectId, user, pageable);
        return ResponseEntity.ok(ApiResponse.success(assignments, "Assignments retrieved successfully"));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_ADMIN')")
    @Operation(summary = "Submit a student solution for an assignment")
    public ResponseEntity<ApiResponse<AssignmentSubmissionDto>> submitAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubmitAssignmentRequest request,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String clientIp = servletRequest.getRemoteAddr();
        AssignmentSubmissionDto submission = assignmentService.submitAssignment(request, user, clientIp);
        return ResponseEntity.ok(ApiResponse.success(submission, "Assignment solution submitted successfully"));
    }

    @GetMapping("/{assignmentId}/submissions")
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    @Operation(summary = "View all submissions for an assignment")
    public ResponseEntity<ApiResponse<List<AssignmentSubmissionDto>>> getSubmissions(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<AssignmentSubmissionDto> submissions = assignmentService.getSubmissionsForAssignment(assignmentId, user);
        return ResponseEntity.ok(ApiResponse.success(submissions, "Submissions retrieved successfully"));
    }

    @PutMapping("/submissions/{submissionId}/grade")
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    @Operation(summary = "Grade a student submission with score and feedback")
    public ResponseEntity<ApiResponse<AssignmentSubmissionDto>> gradeSubmission(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GradeSubmissionRequest request,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String clientIp = servletRequest.getRemoteAddr();
        AssignmentSubmissionDto graded = assignmentService.gradeSubmission(submissionId, request, user, clientIp);
        return ResponseEntity.ok(ApiResponse.success(graded, "Submission graded successfully"));
    }

    @GetMapping("/my-submissions")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_ADMIN')")
    @Operation(summary = "Get current student's assignment submissions and grades")
    public ResponseEntity<ApiResponse<List<AssignmentSubmissionDto>>> getMySubmissions(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<AssignmentSubmissionDto> submissions = assignmentService.getStudentSubmissions(user);
        return ResponseEntity.ok(ApiResponse.success(submissions, "Submissions retrieved successfully"));
    }
}

