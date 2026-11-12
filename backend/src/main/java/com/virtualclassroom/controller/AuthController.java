package com.virtualclassroom.controller;

import com.virtualclassroom.dto.*;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication & Registration Endpoints")
public class AuthController {

    private final AuthService authService;
    private final boolean cookieSecure;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthController(
            AuthService authService,
            @org.springframework.beans.factory.annotation.Value("${app.security.cookie-secure:false}") boolean cookieSecure
    ) {
        this.authService = authService;
        this.cookieSecure = cookieSecure;
    }

    public AuthController(AuthService authService) {
        this(authService, false);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT bearer token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        AuthResponse response = authService.login(request, clientIp);
        if (response.getRefreshToken() != null) {
            setRefreshTokenCookie(servletResponse, response.getRefreshToken());
            if (!isPayloadTransportRequested(servletRequest)) {
                response.setRefreshToken(null);
            }
        }
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
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String token = (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank())
                ? request.getRefreshToken()
                : cookieRefreshToken;

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Refresh token must be provided via request payload or cookie");
        }

        String clientIp = servletRequest.getRemoteAddr();
        AuthResponse response = authService.refreshToken(token, clientIp);
        if (response.getRefreshToken() != null) {
            setRefreshTokenCookie(servletResponse, response.getRefreshToken());
            if (!isPayloadTransportRequested(servletRequest)) {
                response.setRefreshToken(null);
            }
        }
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token and log out")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            HttpServletResponse servletResponse
    ) {
        String token = (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank())
                ? request.getRefreshToken()
                : cookieRefreshToken;

        if (token != null && !token.isBlank()) {
            authService.revokeRefreshToken(token);
        }
        clearRefreshTokenCookie(servletResponse);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password recovery instructions")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        String message = authService.forgotPassword(request.getEmail(), servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(message, message));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using recovery token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        authService.resetPassword(request.getToken(), request.getNewPassword(), servletRequest.getRemoteAddr());
        clearRefreshTokenCookie(servletResponse);
        return ResponseEntity.ok(ApiResponse.success(null, "Password has been reset successfully. Please log in with your new password."));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change password for authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        authService.changePassword(principal.getId(), request.getOldPassword(), request.getNewPassword(), servletRequest.getRemoteAddr());
        clearRefreshTokenCookie(servletResponse);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully. Please log in with your new password."));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private boolean isPayloadTransportRequested(HttpServletRequest request) {
        if (request == null) return false;
        String transport = request.getHeader("X-Token-Transport");
        String clientType = request.getHeader("X-Client-Type");
        return "payload".equalsIgnoreCase(transport) || "mobile".equalsIgnoreCase(clientType);
    }
}
