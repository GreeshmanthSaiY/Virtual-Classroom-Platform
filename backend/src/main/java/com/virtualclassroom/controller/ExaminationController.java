package com.virtualclassroom.controller;

import com.virtualclassroom.dto.ApiResponse;
import com.virtualclassroom.dto.CreateExaminationRequest;
import com.virtualclassroom.dto.ExaminationDto;
import com.virtualclassroom.dto.ExaminationResultDto;
import com.virtualclassroom.dto.RecordGradeRequest;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.ExaminationService;
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
@RequestMapping("/api/v1/examinations")
@Tag(name = "Examinations", description = "Examinations, Assessments & Official Grading Endpoints")
public class ExaminationController {

    private final ExaminationService examinationService;
    private final UserRepository userRepository;

    public ExaminationController(ExaminationService examinationService, UserRepository userRepository) {
        this.examinationService = examinationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    @Operation(summary = "Create an examination/assessment for an assigned subject")
    public ResponseEntity<ApiResponse<ExaminationDto>> createExamination(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateExaminationRequest request,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String clientIp = servletRequest.getRemoteAddr();
        ExaminationDto created = examinationService.createExamination(request, user, clientIp);
        return ResponseEntity.ok(ApiResponse.success(created, "Examination scheduled successfully"));
    }

    @GetMapping("/subject/{subjectId}")
    @Operation(summary = "Get scheduled examinations for a subject (paged)")
    public ResponseEntity<ApiResponse<Page<ExaminationDto>>> getExaminationsForSubject(
            @PathVariable Integer subjectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Page<ExaminationDto> exams = examinationService.getExaminationsForSubjectPaged(subjectId, user, pageable);
        return ResponseEntity.ok(ApiResponse.success(exams, "Examinations retrieved successfully"));
    }

    @PostMapping("/grades")
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    @Operation(summary = "Record or update official examination grade for an enrolled student")
    public ResponseEntity<ApiResponse<ExaminationResultDto>> recordGrade(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RecordGradeRequest request,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String clientIp = servletRequest.getRemoteAddr();
        ExaminationResultDto result = examinationService.recordGrade(request, user, clientIp);
        return ResponseEntity.ok(ApiResponse.success(result, "Student grade recorded successfully"));
    }

    @GetMapping("/{examinationId}/results")
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    @Operation(summary = "Get official marks and results for an examination")
    public ResponseEntity<ApiResponse<List<ExaminationResultDto>>> getResultsForExamination(
            @PathVariable Long examinationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<ExaminationResultDto> results = examinationService.getResultsForExamination(examinationId, user);
        return ResponseEntity.ok(ApiResponse.success(results, "Examination results retrieved successfully"));
    }

    @GetMapping("/my-results")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_ADMIN')")
    @Operation(summary = "Get current student's official examination results and grades")
    public ResponseEntity<ApiResponse<List<ExaminationResultDto>>> getMyResults(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<ExaminationResultDto> results = examinationService.getStudentResults(user);
        return ResponseEntity.ok(ApiResponse.success(results, "Student grades retrieved successfully"));
    }
}

