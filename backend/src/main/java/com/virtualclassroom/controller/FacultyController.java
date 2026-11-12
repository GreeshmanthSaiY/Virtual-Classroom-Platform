package com.virtualclassroom.controller;

import com.virtualclassroom.dto.*;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.FacultyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/faculty")
@PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
@Tag(name = "Faculty Operations", description = "Faculty Course Management & Content Uploads")
public class FacultyController {

    private final FacultyService facultyService;
    private final UserRepository userRepository;

    public FacultyController(FacultyService facultyService, UserRepository userRepository) {
        this.facultyService = facultyService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get aggregated faculty academic dashboard metrics, active sessions and pending submissions")
    public ResponseEntity<ApiResponse<FacultyDashboardDto>> getFacultyDashboard(@AuthenticationPrincipal UserPrincipal principal) {
        FacultyDashboardDto dashboard = facultyService.getFacultyDashboard(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Faculty dashboard retrieved successfully"));
    }

    @GetMapping("/subjects/{subjectId}/students")
    @Operation(summary = "Inspect student roster enrolled in a subject taught by this faculty member")
    public ResponseEntity<ApiResponse<List<UserDto>>> getSubjectStudentRoster(
            @PathVariable Integer subjectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<UserDto> roster = facultyService.getEnrolledStudentsForSubject(subjectId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(roster, "Student roster retrieved successfully"));
    }

    @GetMapping("/subjects")
    @Operation(summary = "Get list of subjects assigned to this faculty member")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getAssignedSubjects(@AuthenticationPrincipal UserPrincipal principal) {
        List<SubjectDto> subjects = facultyService.getAssignedSubjects(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(subjects, "Assigned subjects retrieved"));
    }

    @GetMapping("/materials")
    @Operation(summary = "Get study materials uploaded by this faculty member")
    public ResponseEntity<ApiResponse<List<StudyMaterialDto>>> getMaterials(
            @RequestParam(required = false) Integer subjectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<StudyMaterialDto> materials = facultyService.getMaterialsByFaculty(principal.getId(), subjectId);
        return ResponseEntity.ok(ApiResponse.success(materials, "Study materials retrieved"));
    }

    @GetMapping("/lectures")
    @Operation(summary = "Get video lectures published by this faculty member")
    public ResponseEntity<ApiResponse<List<VideoLectureDto>>> getLectures(
            @RequestParam(required = false) Integer subjectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<VideoLectureDto> lectures = facultyService.getLecturesByFaculty(principal.getId(), subjectId);
        return ResponseEntity.ok(ApiResponse.success(lectures, "Video lectures retrieved"));
    }

    @PostMapping("/materials")
    @Operation(summary = "Upload a new study material resource")
    public ResponseEntity<ApiResponse<StudyMaterialDto>> uploadMaterial(
            @Valid @RequestBody CreateMaterialRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.getReferenceById(principal.getId());
        StudyMaterialDto material = facultyService.uploadStudyMaterial(request, user, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(material, "Study material published successfully"));
    }

    @PostMapping("/lectures")
    @Operation(summary = "Publish a new video lecture")
    public ResponseEntity<ApiResponse<VideoLectureDto>> createLecture(
            @Valid @RequestBody CreateLectureRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.getReferenceById(principal.getId());
        VideoLectureDto lecture = facultyService.createVideoLecture(request, user, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(lecture, "Video lecture published successfully"));
    }

    @DeleteMapping("/materials/{id}")
    @Operation(summary = "Delete an uploaded study material")
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userRepository.getReferenceById(principal.getId());
        facultyService.deleteMaterial(id, user);
        return ResponseEntity.ok(ApiResponse.success(null, "Study material deleted successfully"));
    }

    @DeleteMapping("/lectures/{id}")
    @Operation(summary = "Delete a published video lecture")
    public ResponseEntity<ApiResponse<Void>> deleteLecture(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userRepository.getReferenceById(principal.getId());
        facultyService.deleteLecture(id, user);
        return ResponseEntity.ok(ApiResponse.success(null, "Video lecture deleted successfully"));
    }
}
