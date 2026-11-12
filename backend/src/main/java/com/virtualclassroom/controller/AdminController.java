package com.virtualclassroom.controller;

import com.virtualclassroom.dto.*;
import com.virtualclassroom.model.AuditLog;
import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.AdminService;
import com.virtualclassroom.service.AuditLogService;
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
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Administrator Operations", description = "Admin User Approvals & System Management")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public AdminController(AdminService adminService, AuditLogService auditLogService, UserRepository userRepository) {
        this.adminService = adminService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }


    @GetMapping("/metrics")
    @Operation(summary = "Get platform-wide analytics and entity counts")
    public ResponseEntity<ApiResponse<PlatformMetricsDto>> getPlatformMetrics() {
        PlatformMetricsDto metrics = adminService.getPlatformMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics, "Metrics calculated"));
    }

    @GetMapping("/students/pending")
    @Operation(summary = "List all pending student registration requests")
    public ResponseEntity<ApiResponse<List<UserDto>>> getPendingStudents() {
        List<UserDto> pending = adminService.getPendingStudents();
        return ResponseEntity.ok(ApiResponse.success(pending, "Pending student requests retrieved"));
    }

    @GetMapping("/faculty/pending")
    @Operation(summary = "List all pending faculty registration requests")
    public ResponseEntity<ApiResponse<List<UserDto>>> getPendingFaculty() {
        List<UserDto> pending = adminService.getPendingFaculty();
        return ResponseEntity.ok(ApiResponse.success(pending, "Pending faculty requests retrieved"));
    }

    @GetMapping("/users")
    @Operation(summary = "Get paginated user list filtered by role")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsers(
            @RequestParam(defaultValue = "ROLE_STUDENT") Role role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<UserDto> users = adminService.getAllUsersByRole(role, pageable);
        return ResponseEntity.ok(ApiResponse.success(users, "Users list retrieved"));
    }

    @GetMapping("/users/search")
    @Operation(summary = "Search users by keyword, role, and account status with pagination")
    public ResponseEntity<ApiResponse<Page<UserDto>>> searchUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) com.virtualclassroom.model.UserStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<UserDto> users = adminService.searchUsers(q, role, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(users, "User search completed"));
    }

    @PostMapping("/users/{userId}/suspend")
    @Operation(summary = "Suspend user account access and terminate active sessions")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable Long userId,
            @Valid @RequestBody SuspendUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User admin = userRepository.getReferenceById(principal.getId());
        adminService.suspendUser(userId, request.getReason(), admin, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(null, "User suspended successfully"));
    }

    @PostMapping("/users/{userId}/reactivate")
    @Operation(summary = "Reactivate a suspended user account")
    public ResponseEntity<ApiResponse<Void>> reactivateUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User admin = userRepository.getReferenceById(principal.getId());
        adminService.reactivateUser(userId, admin, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(null, "User reactivated successfully"));
    }

    @PostMapping("/subjects/{subjectId}/assign-faculty")
    @Operation(summary = "Assign a faculty member as instructor to a subject")
    public ResponseEntity<ApiResponse<Void>> assignFacultyToSubject(
            @PathVariable Integer subjectId,
            @Valid @RequestBody AssignFacultyRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User admin = userRepository.getReferenceById(principal.getId());
        adminService.assignFacultyToSubject(subjectId, request.getFacultyId(), request.getAcademicTerm(), admin, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(null, "Faculty assigned to subject successfully"));
    }

    @PostMapping({"/approvals", "/approve"})
    @Operation(summary = "Approve user registration requests")
    public ResponseEntity<ApiResponse<Void>> approveUsers(
            @Valid @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User admin = userRepository.getReferenceById(principal.getId());
        adminService.approveUsers(request.getUserIds(), admin, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(null, "Users approved successfully"));
    }

    @PostMapping({"/rejections", "/reject"})
    @Operation(summary = "Reject user registration requests with documented reason")
    public ResponseEntity<ApiResponse<Void>> rejectUsers(
            @Valid @RequestBody RejectRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User admin = userRepository.getReferenceById(principal.getId());
        adminService.rejectUsers(request.getUserIds(), request.getReason(), admin, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(null, "Users rejected successfully"));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "View security and operational audit trail")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @PageableDefault(size = 30) Pageable pageable
    ) {
        Page<AuditLog> logs = auditLogService.getAuditLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success(logs, "Audit logs retrieved"));
    }
}
