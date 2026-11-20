package com.virtualclassroom.bootstrap;

import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.User;
import com.virtualclassroom.model.UserStatus;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Secure, one-time, idempotent, staging-only initial administrator bootstrap.
 *
 * Safeguards:
 * - Annotated strictly with @Profile("staging"); never loaded in prod, local, dev, or test.
 * - Runtime profile guard fails closed if 'prod'/'production' is active or 'staging' is absent.
 * - Disabled by default (app.bootstrap.staging-admin.enabled=false).
 * - Requires externally supplied email and strong password (>= 14 chars with complexity);
 *   zero hardcoded credentials and zero default passwords.
 * - Strictly idempotent: if any ROLE_ADMIN or the target email already exists, performs no changes.
 */
@Component
@Profile("staging")
public class StagingAdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StagingAdminBootstrapRunner.class);

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Set<String> PROHIBITED_DEMO_EMAILS = Set.of(
            "admin@virtualclassroom.edu",
            "dr.jeevan@virtualclassroom.edu",
            "gaurav.shukla@virtualclassroom.edu"
    );

    private static final int MIN_PASSWORD_LENGTH = 14;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final Environment environment;
    private final boolean enabled;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminFullName;

    public StagingAdminBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            Environment environment,
            @Value("${app.bootstrap.staging-admin.enabled:false}") boolean enabled,
            @Value("${app.bootstrap.staging-admin.email:}") String adminEmail,
            @Value("${app.bootstrap.staging-admin.password:}") String adminPassword,
            @Value("${app.bootstrap.staging-admin.full-name:Staging Administrator}") String adminFullName
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.environment = environment;
        this.enabled = enabled;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminFullName = adminFullName;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            log.info("Staging admin bootstrap is disabled (app.bootstrap.staging-admin.enabled=false).");
            return;
        }

        validateActiveProfiles();

        String normalizedEmail = validateAndNormalizeEmail(adminEmail);
        validatePasswordStrength(adminPassword);
        String resolvedName = (adminFullName == null || adminFullName.isBlank())
                ? "Staging Administrator"
                : adminFullName.trim();

        if (userRepository.countByRole(Role.ROLE_ADMIN) > 0) {
            log.info("Staging admin bootstrap skipped: an administrator account already exists (idempotent no-op).");
            return;
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.info("Staging admin bootstrap skipped: target email already exists (idempotent no-op).");
            return;
        }

        User admin = User.builder()
                .email(normalizedEmail)
                .fullName(resolvedName)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ROLE_ADMIN)
                .status(UserStatus.APPROVED)
                .build();

        User saved = userRepository.save(admin);
        auditLogService.logAction(
                saved,
                "ADMIN_BOOTSTRAP_CREATED",
                "users",
                saved.getId() != null ? saved.getId().toString() : "pending",
                "One-time staging administrator bootstrap completed. Disable STAGING_ADMIN_BOOTSTRAP_ENABLED immediately.",
                "system-bootstrap"
        );

        log.info("Staging administrator bootstrap completed for {}. Disable STAGING_ADMIN_BOOTSTRAP_ENABLED and unset STAGING_ADMIN_PASSWORD.", normalizedEmail);
    }

    private void validateActiveProfiles() {
        String[] activeProfiles = environment != null ? environment.getActiveProfiles() : new String[0];
        boolean hasProd = Arrays.stream(activeProfiles)
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
        if (hasProd) {
            throw new IllegalStateException("StagingAdminBootstrapRunner is strictly prohibited when production profile is active.");
        }
        boolean hasStaging = Arrays.stream(activeProfiles)
                .anyMatch(p -> "staging".equalsIgnoreCase(p));
        if (!hasStaging) {
            throw new IllegalStateException("StagingAdminBootstrapRunner requires the 'staging' profile to be active.");
        }
    }

    private String validateAndNormalizeEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new IllegalStateException("STAGING_ADMIN_EMAIL must be configured when staging admin bootstrap is enabled.");
        }
        String normalized = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalStateException("STAGING_ADMIN_EMAIL is not a valid email address.");
        }
        if (PROHIBITED_DEMO_EMAILS.contains(normalized)) {
            throw new IllegalStateException("STAGING_ADMIN_EMAIL cannot reuse a prohibited development demo account email.");
        }
        return normalized;
    }

    private void validatePasswordStrength(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("STAGING_ADMIN_PASSWORD must be provided externally and be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }
        boolean hasUpper = rawPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = rawPassword.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = rawPassword.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = rawPassword.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new IllegalStateException("STAGING_ADMIN_PASSWORD must include uppercase, lowercase, numeric, and special characters.");
        }
    }
}
