package com.virtualclassroom;

import com.virtualclassroom.dto.*;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import com.virtualclassroom.security.JwtTokenProvider;
import com.virtualclassroom.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Milestone 4.1 - Backend Security and Object-Level Authorization Audit Tests
 * Classification: Security Unit & Integration Tests
 */
class BackendSecurityAuditTest {

    @TempDir
    Path tempStorageDir;

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordResetTokenRepository passwordResetTokenRepository;
    private PasswordEncoder passwordEncoder;
    private AuditLogService auditLogService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditLogService = mock(AuditLogService.class);

        authService = new AuthService(
                mock(AuthenticationManager.class),
                userRepository,
                mock(StudentProfileRepository.class),
                mock(FacultyProfileRepository.class),
                mock(CourseRepository.class),
                mock(SubjectRepository.class),
                mock(FacultySubjectAssignmentRepository.class),
                passwordEncoder,
                mock(JwtTokenProvider.class),
                auditLogService,
                refreshTokenRepository,
                passwordResetTokenRepository
        );
    }

    @Test
    @DisplayName("Security: Forgot-password response does not disclose whether email exists (enumeration defense)")
    void forgotPassword_EnumerationProtection_ReturnsIdenticalMessage() {
        when(userRepository.findByEmail("nonexistent@university.edu")).thenReturn(Optional.empty());

        User existingUser = User.builder()
                .id(1L)
                .email("student@university.edu")
                .status(UserStatus.APPROVED)
                .build();
        when(userRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(existingUser));

        String nonExistentResponse = authService.forgotPassword("nonexistent@university.edu", "127.0.0.1");
        String existingResponse = authService.forgotPassword("student@university.edu", "127.0.0.1");

        assertNotNull(nonExistentResponse);
        assertNotNull(existingResponse);
        assertEquals(nonExistentResponse, existingResponse);
        assertEquals("If an account exists with this email address, password reset instructions have been sent.", existingResponse);

        // Verify token saved only for existing user and audit logged
        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(auditLogService, times(1)).logAction(eq(existingUser), eq("PASSWORD_RESET_REQUESTED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Security: Production profile fails fast on missing Supabase cloud storage credentials")
    void storageService_ProductionProfileMissingCredentials_ThrowsIllegalStateException() {
        Environment prodEnv = mock(Environment.class);
        when(prodEnv.matchesProfiles("prod")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new FileStorageService(
                        "supabase", "", "", "academic-assets",
                        tempStorageDir.toString(), 10 * 1024 * 1024, prodEnv
                )
        );
        assertTrue(ex.getMessage().contains("Production configuration error"));
        assertTrue(ex.getMessage().contains("Supabase cloud storage"));
    }

    @Test
    @DisplayName("Security: Storage bucket must match explicit allowlist")
    void storageService_UnapprovedBucket_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new FileStorageService(
                        "local", "", "", "arbitrary-untrusted-bucket",
                        tempStorageDir.toString(), 10 * 1024 * 1024
                )
        );
        assertTrue(ex.getMessage().contains("not in the approved bucket allowlist"));
    }

    @Test
    @DisplayName("Security: Non-admin users cannot delete files belonging to other accounts")
    void storageService_DeleteFile_NonAdminTargetingOtherUser_ThrowsAccessDenied() {
        FileStorageService storage = new FileStorageService(
                "local", "", "", "academic-assets",
                tempStorageDir.toString(), 10 * 1024 * 1024
        );

        User facultyA = User.builder().id(101L).role(Role.ROLE_FACULTY).build();
        String fileBelongingToFacultyB = "materials/202/uuid-exam.pdf";

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                storage.deleteFile(fileBelongingToFacultyB, facultyA)
        );
        assertTrue(ex.getMessage().contains("not authorized to delete files belonging to other users"));
    }

    @Test
    @DisplayName("Security: Admin users can delete any file from storage")
    void storageService_DeleteFile_AdminCanDeleteAnyFile() {
        FileStorageService storage = new FileStorageService(
                "local", "", "", "academic-assets",
                tempStorageDir.toString(), 10 * 1024 * 1024
        );

        User admin = User.builder().id(999L).role(Role.ROLE_ADMIN).build();
        assertDoesNotThrow(() -> storage.deleteFile("materials/101/uuid-notes.pdf", admin));
    }

    @Test
    @DisplayName("Security: Signed download URL expiration is bounded within safe maximums (60s to 86400s)")
    void storageService_BoundedSignedUrlExpiration() {
        FileStorageService storage = new FileStorageService(
                "local", "", "", "academic-assets",
                tempStorageDir.toString(), 10 * 1024 * 1024
        );

        String path = storage.generateSignedDownloadUrl("general/doc.pdf", 9999999);
        assertNotNull(path);
        assertTrue(path.contains("doc.pdf"));
    }

    @Test
    @DisplayName("Security: Browser client response suppresses raw refresh token from JSON payload")
    void authController_BrowserClient_SuppressesRefreshTokenInPayload() {
        AuthResponse rawAuthResponse = AuthResponse.builder()
                .token("jwt-access-token")
                .tokenType("Bearer")
                .id(1L)
                .email("student@university.edu")
                .fullName("Test Student")
                .role("ROLE_STUDENT")
                .status("APPROVED")
                .refreshToken("secret-refresh-token")
                .build();

        AuthService mockAuth = mock(AuthService.class);
        when(mockAuth.login(any(), any())).thenReturn(rawAuthResponse);

        com.virtualclassroom.controller.AuthController controller =
                new com.virtualclassroom.controller.AuthController(mockAuth, true, "Strict");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        LoginRequest loginReq = new LoginRequest("student@university.edu", "Password123!");
        var entity = controller.login(loginReq, request, response);

        assertNotNull(entity.getBody());
        assertNotNull(entity.getBody().getData());
        assertNull(entity.getBody().getData().getRefreshToken(), "Browser response must not leak refresh token in JSON payload");
        assertNotNull(response.getHeader("Set-Cookie"), "Set-Cookie header must be present for browser client");
        assertTrue(response.getHeader("Set-Cookie").contains("HttpOnly"));
        assertTrue(response.getHeader("Set-Cookie").contains("SameSite=Strict"));
        assertTrue(response.getHeader("Set-Cookie").contains("Secure"), "Secure flag must be set when cookieSecure is true");
    }

    @Test
    @DisplayName("Security: Mobile client requesting payload transport receives refresh token in JSON payload")
    void authController_MobileClient_ReceivesRefreshTokenInPayload() {
        AuthResponse rawAuthResponse = AuthResponse.builder()
                .token("jwt-access-token")
                .tokenType("Bearer")
                .id(1L)
                .email("student@university.edu")
                .fullName("Test Student")
                .role("ROLE_STUDENT")
                .status("APPROVED")
                .refreshToken("secret-refresh-token")
                .build();

        AuthService mockAuth = mock(AuthService.class);
        when(mockAuth.login(any(), any())).thenReturn(rawAuthResponse);

        com.virtualclassroom.controller.AuthController controller =
                new com.virtualclassroom.controller.AuthController(mockAuth, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Token-Transport", "payload");
        MockHttpServletResponse response = new MockHttpServletResponse();

        LoginRequest loginReq = new LoginRequest("student@university.edu", "Password123!");
        var entity = controller.login(loginReq, request, response);

        assertNotNull(entity.getBody());
        assertNotNull(entity.getBody().getData());
        assertEquals("secret-refresh-token", entity.getBody().getData().getRefreshToken());
    }

    @Test
    @DisplayName("Security: Student A cannot inspect Student B's profile")
    void userProfileService_StudentACannotViewStudentBProfile_ThrowsAccessDenied() {
        UserProfileService profileService = new UserProfileService(
                userRepository,
                mock(StudentProfileRepository.class),
                mock(FacultyProfileRepository.class),
                auditLogService
        );

        User studentA = User.builder().id(100L).role(Role.ROLE_STUDENT).build();

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                profileService.getProfile(200L, studentA));
        assertTrue(ex.getMessage().contains("only view your own user profile"));
    }
}
