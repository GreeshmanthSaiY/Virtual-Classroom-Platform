package com.virtualclassroom.controller;

import com.virtualclassroom.dto.ApiResponse;
import com.virtualclassroom.dto.CreateLiveSessionRequest;
import com.virtualclassroom.dto.LiveSessionDto;
import com.virtualclassroom.model.LiveSessionStatus;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.LiveSessionService;
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

@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Live Sessions", description = "Virtual Classroom Live Meeting Endpoints")
public class LiveSessionController {

    private final LiveSessionService liveSessionService;
    private final UserRepository userRepository;

    public LiveSessionController(LiveSessionService liveSessionService, UserRepository userRepository) {
        this.liveSessionService = liveSessionService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    @Operation(summary = "Schedule a live classroom meeting for an assigned subject")
    public ResponseEntity<ApiResponse<LiveSessionDto>> scheduleSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateLiveSessionRequest request,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String clientIp = servletRequest.getRemoteAddr();
        LiveSessionDto scheduled = liveSessionService.scheduleLiveSession(request, user, clientIp);
        return ResponseEntity.ok(ApiResponse.success(scheduled, "Live session scheduled successfully"));
    }

    @GetMapping("/subject/{subjectId}")
    @Operation(summary = "Get live sessions for an academic subject (paged)")
    public ResponseEntity<ApiResponse<Page<LiveSessionDto>>> getSessionsForSubject(
            @PathVariable Integer subjectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Page<LiveSessionDto> sessions = liveSessionService.getSessionsForSubjectPaged(subjectId, user, pageable);
        return ResponseEntity.ok(ApiResponse.success(sessions, "Live sessions retrieved successfully"));
    }

    @PutMapping("/{sessionId}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    @Operation(summary = "Update session status (LIVE, COMPLETED, CANCELLED)")
    public ResponseEntity<ApiResponse<LiveSessionDto>> updateStatus(
            @PathVariable Long sessionId,
            @RequestParam LiveSessionStatus status,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String clientIp = servletRequest.getRemoteAddr();
        LiveSessionDto updated = liveSessionService.updateSessionStatus(sessionId, status, user, clientIp);
        return ResponseEntity.ok(ApiResponse.success(updated, "Session status updated to " + status));
    }
}

