package com.virtualclassroom;

import com.virtualclassroom.bootstrap.StagingAdminBootstrapRunner;
import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.User;
import com.virtualclassroom.model.UserStatus;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.service.AuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StagingAdminBootstrapRunnerTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("StagingAdminBootstrapRunner is strictly annotated with @Profile('staging')")
    void runnerHasStrictStagingProfileAnnotation() {
        Profile annotation = StagingAdminBootstrapRunner.class.getAnnotation(Profile.class);
        assertNotNull(annotation, "Runner must declare @Profile");
        assertArrayEquals(new String[]{"staging"}, annotation.value());
    }

    @Test
    @DisplayName("Bootstrap is a no-op when disabled (default behavior)")
    void disabledByDefaultDoesNothing() {
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        MockEnvironment env = new MockEnvironment().withProperty("dummy", "1");
        env.setActiveProfiles("staging");

        StagingAdminBootstrapRunner runner = new StagingAdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogService, env,
                false, "", "", "Staging Administrator"
        );

        runner.run();
        verifyNoInteractions(userRepository, auditLogService);
    }

    @Test
    @DisplayName("Bootstrap fails closed if production profile is active")
    void refusesToRunUnderProductionProfile() {
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("staging", "prod");

        StagingAdminBootstrapRunner runner = new StagingAdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogService, env,
                true, "ops.admin@example.org", "Str0ng!Staging#2026", "Ops Admin"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, runner::run);
        assertTrue(ex.getMessage().contains("prohibited"));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Bootstrap fails closed when staging profile is not active")
    void refusesToRunWithoutStagingProfile() {
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");

        StagingAdminBootstrapRunner runner = new StagingAdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogService, env,
                true, "ops.admin@example.org", "Str0ng!Staging#2026", "Ops Admin"
        );

        assertThrows(IllegalStateException.class, runner::run);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Bootstrap fails closed on missing password, weak password, or prohibited demo email")
    void rejectsWeakCredentialsAndDemoEmail() {
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("staging");

        StagingAdminBootstrapRunner weakPasswordRunner = new StagingAdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogService, env,
                true, "ops.admin@example.org", "short", "Ops Admin"
        );
        assertThrows(IllegalStateException.class, weakPasswordRunner::run);

        StagingAdminBootstrapRunner demoEmailRunner = new StagingAdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogService, env,
                true, "admin@virtualclassroom.edu", "Str0ng!Staging#2026", "Ops Admin"
        );
        assertThrows(IllegalStateException.class, demoEmailRunner::run);

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Bootstrap creates approved BCrypt-hashed ROLE_ADMIN account and logs audit event")
    void createsStagingAdminWhenEnabledAndValid() {
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("staging");

        when(userRepository.countByRole(Role.ROLE_ADMIN)).thenReturn(0L);
        when(userRepository.existsByEmail("ops.admin@example.org")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        String rawPassword = "Str0ng!Staging#2026";
        StagingAdminBootstrapRunner runner = new StagingAdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogService, env,
                true, "OPS.Admin@Example.org", rawPassword, "Staging Ops Admin"
        );

        runner.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("ops.admin@example.org", saved.getEmail());
        assertEquals("Staging Ops Admin", saved.getFullName());
        assertEquals(Role.ROLE_ADMIN, saved.getRole());
        assertEquals(UserStatus.APPROVED, saved.getStatus());
        assertNotEquals(rawPassword, saved.getPasswordHash());
        assertTrue(passwordEncoder.matches(rawPassword, saved.getPasswordHash()));

        verify(auditLogService).logAction(
                eq(saved),
                eq("ADMIN_BOOTSTRAP_CREATED"),
                eq("users"),
                eq("42"),
                anyString(),
                eq("system-bootstrap")
        );
    }

    @Test
    @DisplayName("Bootstrap is strictly idempotent when an administrator already exists")
    void idempotentWhenAdminAlreadyExists() {
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("staging");

        when(userRepository.countByRole(Role.ROLE_ADMIN)).thenReturn(1L);

        StagingAdminBootstrapRunner runner = new StagingAdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogService, env,
                true, "ops.admin@example.org", "Str0ng!Staging#2026", "Staging Ops Admin"
        );

        runner.run();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }
}
