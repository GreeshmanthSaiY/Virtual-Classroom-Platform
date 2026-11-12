package com.virtualclassroom.controller;

import com.virtualclassroom.dto.*;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication & Registration Endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT bearer token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        AuthResponse response = authService.login(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/register/student")
    @Operation(summary = "Submit student registration request awaiting admin approval")
    public ResponseEntity<ApiResponse<UserDto>> registerStudent(
            @Valid @RequestBody RegisterStudentRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        UserDto user = authService.registerStudent(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success(user, "Student registration submitted successfully. Your account is pending Administrator approval."));
    }

    @PostMapping("/register/faculty")
    @Operation(summary = "Submit faculty registration request awaiting admin approval")
    public ResponseEntity<ApiResponse<UserDto>> registerFaculty(
            @Valid @RequestBody RegisterFacultyRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        UserDto user = authService.registerFaculty(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success(user, "Faculty registration submitted successfully. Your account is pending Administrator approval."));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserDto user = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.success(user, "Profile retrieved successfully"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        AuthResponse response = authService.refreshToken(request.getRefreshToken(), clientIp);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token and log out")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        authService.revokeRefreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }
}
