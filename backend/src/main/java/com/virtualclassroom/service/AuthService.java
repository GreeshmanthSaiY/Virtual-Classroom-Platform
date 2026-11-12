package com.virtualclassroom.service;

import com.virtualclassroom.dto.*;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import com.virtualclassroom.security.JwtTokenProvider;
import com.virtualclassroom.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final FacultySubjectAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            FacultyProfileRepository facultyProfileRepository,
            CourseRepository courseRepository,
            SubjectRepository subjectRepository,
            FacultySubjectAssignmentRepository assignmentRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            AuditLogService auditLogService
    ) {
        this(authenticationManager, userRepository, studentProfileRepository, facultyProfileRepository,
                courseRepository, subjectRepository, assignmentRepository, passwordEncoder, tokenProvider,
                auditLogService, null, null);
    }

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            FacultyProfileRepository facultyProfileRepository,
            CourseRepository courseRepository,
            SubjectRepository subjectRepository,
            FacultySubjectAssignmentRepository assignmentRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            AuditLogService auditLogService,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this(authenticationManager, userRepository, studentProfileRepository, facultyProfileRepository,
                courseRepository, subjectRepository, assignmentRepository, passwordEncoder, tokenProvider,
                auditLogService, refreshTokenRepository, null);
    }

    @Autowired
    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            FacultyProfileRepository facultyProfileRepository,
            CourseRepository courseRepository,
            SubjectRepository subjectRepository,
            FacultySubjectAssignmentRepository assignmentRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            AuditLogService auditLogService,
            @Autowired(required = false) RefreshTokenRepository refreshTokenRepository,
            @Autowired(required = false) PasswordResetTokenRepository passwordResetTokenRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.facultyProfileRepository = facultyProfileRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditLogService = auditLogService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }


    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getStatus() == UserStatus.PENDING) {
            throw new IllegalStateException("Your account registration request is awaiting Administrator approval.");
        }

        if (user.getStatus() == UserStatus.REJECTED) {
            String reason = user.getRejectionReason() != null ? user.getRejectionReason() : "Requirements not met.";
            throw new IllegalStateException("Your registration request was rejected by Administrator: " + reason);
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("Your account has been suspended by Administrator.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String jwt = tokenProvider.generateToken(authentication);

        String rawRefreshToken = null;
        if (refreshTokenRepository != null) {
            rawRefreshToken = generateSecureToken();
            String hashedToken = hashToken(rawRefreshToken);
            RefreshToken refreshToken = RefreshToken.builder()
                    .user(user)
                    .tokenHash(hashedToken)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .build();
            refreshTokenRepository.save(refreshToken);
        }

        auditLogService.logAction(user, "USER_LOGIN", "users", user.getId().toString(), "Successful login", ipAddress);

        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .refreshToken(rawRefreshToken)
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(String rawToken, String ipAddress) {
        if (refreshTokenRepository == null) {
            throw new IllegalStateException("Refresh token repository is not configured");
        }

        String hashedToken = hashToken(rawToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        User user = storedToken.getUser();

        // REPLAY DETECTION / COMPROMISED TOKEN REUSE
        if (storedToken.isRevoked()) {
            // A revoked token was replayed. Family revocation breach mitigation:
            refreshTokenRepository.revokeAllActiveByUserId(user.getId());
            auditLogService.logAction(user, "TOKEN_REPLAY_DETECTED", "RefreshToken",
                    storedToken.getId().toString(), "Revoked token replayed. Terminated all active user sessions.", ipAddress);
            throw new BadCredentialsException("Security breach warning: Revoked session token reuse detected. All active sessions have been invalidated.");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token has expired");
        }

        if (user.getStatus() != UserStatus.APPROVED) {
            throw new IllegalStateException("Account is not active (status: " + user.getStatus() + ")");
        }

        // Revoke the old token (rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Issue new JWT access token and new cryptographically strong refresh token
        String newJwt = tokenProvider.generateTokenFromUser(user);
        String newRawRefreshToken = generateSecureToken();
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(newRawRefreshToken))
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(newRefreshToken);

        auditLogService.logAction(user, "TOKEN_REFRESH", "users", user.getId().toString(), "Rotated refresh token", ipAddress);

        return AuthResponse.builder()
                .token(newJwt)
                .tokenType("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .refreshToken(newRawRefreshToken)
                .build();
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (refreshTokenRepository != null && rawToken != null && !rawToken.isBlank()) {
            String hashedToken = hashToken(rawToken);
            refreshTokenRepository.findByTokenHash(hashedToken).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Transactional
    public String forgotPassword(String email, String ipAddress) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required");
        }
        String normalizedEmail = email.trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (user.getStatus() == UserStatus.APPROVED && passwordResetTokenRepository != null) {
                // Invalidate existing pending tokens
                passwordResetTokenRepository.invalidateAllActiveByUserId(user.getId());

                String rawToken = generateSecureToken();
                String hashedToken = hashToken(rawToken);
                PasswordResetToken resetToken = PasswordResetToken.builder()
                        .user(user)
                        .tokenHash(hashedToken)
                        .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                        .used(false)
                        .build();
                passwordResetTokenRepository.save(resetToken);

                auditLogService.logAction(user, "PASSWORD_RESET_REQUESTED", "users",
                        user.getId().toString(), "Password reset token requested", ipAddress);
            }
        });

        return "If an account exists with this email address, password reset instructions have been sent.";
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword, String ipAddress) {
        if (passwordResetTokenRepository == null) {
            throw new IllegalStateException("Password reset repository is not configured");
        }
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Password reset token is required");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        String hashedToken = hashToken(rawToken);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired password reset token"));

        if (resetToken.isUsed()) {
            throw new BadCredentialsException("This password reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("This password reset token has expired");
        }

        User user = resetToken.getUser();
        if (user.getStatus() != UserStatus.APPROVED) {
            throw new IllegalStateException("Account is not active (status: " + user.getStatus() + ")");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Invalidate all active user sessions for security
        if (refreshTokenRepository != null) {
            refreshTokenRepository.revokeAllActiveByUserId(user.getId());
        }

        auditLogService.logAction(user, "PASSWORD_RESET_COMPLETED", "users",
                user.getId().toString(), "Password successfully reset via token", ipAddress);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword, String ipAddress) {
        if (userId == null) {
            throw new IllegalArgumentException("User identifier is required");
        }
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new BadCredentialsException("Current password is required");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }
        if (oldPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all refresh tokens to terminate all active sessions across other devices
        if (refreshTokenRepository != null) {
            refreshTokenRepository.revokeAllActiveByUserId(user.getId());
        }

        auditLogService.logAction(user, "PASSWORD_CHANGED", "users",
                user.getId().toString(), "Password changed successfully", ipAddress);
    }

    public String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    @Transactional
    public UserDto registerStudent(RegisterStudentRequest request, String ipAddress) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        if (studentProfileRepository.existsByRollNumber(request.getRollNumber())) {
            throw new IllegalArgumentException("This roll number is already registered.");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid course selected."));

        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.PENDING)
                .build();

        User savedUser = userRepository.save(user);

        StudentProfile profile = StudentProfile.builder()
                .user(savedUser)
                .rollNumber(request.getRollNumber().trim())
                .course(course)
                .currentYear(request.getYear())
                .build();

        studentProfileRepository.save(profile);

        auditLogService.logAction(savedUser, "STUDENT_REGISTRATION", "users", savedUser.getId().toString(),
                "New student registration submitted for approval", ipAddress);

        return toUserDto(savedUser, profile, null);
    }

    @Transactional
    public UserDto registerFaculty(RegisterFacultyRequest request, String ipAddress) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        if (facultyProfileRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new IllegalArgumentException("This employee ID is already registered.");
        }

        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(Role.ROLE_FACULTY)
                .status(UserStatus.PENDING)
                .build();

        User savedUser = userRepository.save(user);

        FacultyProfile profile = FacultyProfile.builder()
                .user(savedUser)
                .employeeId(request.getEmployeeId().trim())
                .department(request.getDepartment().trim())
                .build();

        FacultyProfile savedProfile = facultyProfileRepository.save(profile);

        if (request.getInitialSubjectIds() != null && !request.getInitialSubjectIds().isEmpty()) {
            for (Integer subjectId : request.getInitialSubjectIds()) {
                subjectRepository.findById(subjectId).ifPresent(subject -> {
                    FacultySubjectAssignment assignment = FacultySubjectAssignment.builder()
                            .facultyId(savedUser.getId())
                            .subjectId(subject.getId())
                            .academicTerm("2026-ACTIVE")
                            .facultyProfile(savedProfile)
                            .subject(subject)
                            .build();
                    assignmentRepository.save(assignment);
                });
            }
        }

        auditLogService.logAction(savedUser, "FACULTY_REGISTRATION", "users", savedUser.getId().toString(),
                "New faculty registration submitted for approval", ipAddress);

        return toUserDto(savedUser, null, savedProfile);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        StudentProfile studentProfile = user.getStudentProfile();
        FacultyProfile facultyProfile = user.getFacultyProfile();

        return toUserDto(user, studentProfile, facultyProfile);
    }

    private UserDto toUserDto(User user, StudentProfile student, FacultyProfile faculty) {
        UserDto.UserDtoBuilder builder = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .rejectionReason(user.getRejectionReason())
                .createdAt(user.getCreatedAt());

        if (student != null) {
            builder.rollNumber(student.getRollNumber())
                    .courseName(student.getCourse() != null ? student.getCourse().getName() : null)
                    .currentYear(student.getCurrentYear());
        }

        if (faculty != null) {
            builder.employeeId(faculty.getEmployeeId())
                    .department(faculty.getDepartment());
        }

        return builder.build();
    }
}
