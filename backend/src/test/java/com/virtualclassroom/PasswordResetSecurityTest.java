package com.virtualclassroom;

import com.virtualclassroom.dto.ChangePasswordRequest;
import com.virtualclassroom.dto.ResetPasswordRequest;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.PasswordResetTokenRepository;
import com.virtualclassroom.repository.RefreshTokenRepository;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.service.AuditLogService;
import com.virtualclassroom.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetSecurityTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository resetTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditLogService auditLogService;

    private AuthService authService;
    private User approvedUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                null, userRepository, null, null, null, null, null,
                passwordEncoder, null, auditLogService, refreshTokenRepository, resetTokenRepository
        );

        approvedUser = User.builder()
                .id(100L)
                .email("student@university.edu")
                .fullName("Legit Student")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.APPROVED)
                .passwordHash("$2a$12$oldHashedPassword")
                .build();
    }

    @Test
    @DisplayName("Forgot password issues single-use token and invalidates prior tokens")
    void forgotPassword_GeneratesSecureHashedToken() {
        when(userRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(approvedUser));

        String result = authService.forgotPassword("student@university.edu", "127.0.0.1");

        assertNotNull(result);
        verify(resetTokenRepository).invalidateAllActiveByUserId(100L);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertEquals(100L, savedToken.getUser().getId());
        assertFalse(savedToken.isUsed());
        assertNotNull(savedToken.getTokenHash());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Reset password verifies token, hashes new password, marks token used, and terminates active sessions")
    void resetPassword_ValidToken_UpdatesPasswordAndRevokesSessions() {
        String rawToken = "my_secure_recovery_token";
        String hashedToken = authService.hashToken(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .id(55L)
                .user(approvedUser)
                .tokenHash(hashedToken)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();

        when(resetTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewStrongPassword123!")).thenReturn("$2a$12$newHashedPassword");

        authService.resetPassword(rawToken, "NewStrongPassword123!", "127.0.0.1");

        assertEquals("$2a$12$newHashedPassword", approvedUser.getPasswordHash());
        assertTrue(token.isUsed());
        verify(resetTokenRepository).save(token);
        verify(userRepository).save(approvedUser);
        verify(refreshTokenRepository).revokeAllActiveByUserId(100L);
    }

    @Test
    @DisplayName("Reset password rejects used token (replay protection)")
    void resetPassword_ReplayedToken_ThrowsBadCredentials() {
        String rawToken = "already_used_token";
        String hashedToken = authService.hashToken(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .id(56L)
                .user(approvedUser)
                .tokenHash(hashedToken)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(true)
                .build();

        when(resetTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                authService.resetPassword(rawToken, "NewStrongPassword123!", "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("already been used"));
        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(any());
    }

    @Test
    @DisplayName("Reset password rejects expired token")
    void resetPassword_ExpiredToken_ThrowsBadCredentials() {
        String rawToken = "expired_token";
        String hashedToken = authService.hashToken(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .id(57L)
                .user(approvedUser)
                .tokenHash(hashedToken)
                .expiresAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .used(false)
                .build();

        when(resetTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                authService.resetPassword(rawToken, "NewStrongPassword123!", "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("Change password verifies current password and invalidates active refresh tokens")
    void changePassword_ValidCredentials_UpdatesPasswordAndRevokesSessions() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(approvedUser));
        when(passwordEncoder.matches("CorrectOldPassword123!", "$2a$12$oldHashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("BrandNewPassword456!")).thenReturn("$2a$12$brandNewHash");

        authService.changePassword(100L, "CorrectOldPassword123!", "BrandNewPassword456!", "127.0.0.1");

        assertEquals("$2a$12$brandNewHash", approvedUser.getPasswordHash());
        verify(userRepository).save(approvedUser);
        verify(refreshTokenRepository).revokeAllActiveByUserId(100L);
    }

    @Test
    @DisplayName("Change password rejects incorrect current password")
    void changePassword_WrongCurrentPassword_ThrowsBadCredentials() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(approvedUser));
        when(passwordEncoder.matches("WrongPassword!", "$2a$12$oldHashedPassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () ->
                authService.changePassword(100L, "WrongPassword!", "BrandNewPassword456!", "127.0.0.1")
        );
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(any());
    }
}
