package com.virtualclassroom.service;

import com.virtualclassroom.dto.*;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import com.virtualclassroom.security.JwtTokenProvider;
import com.virtualclassroom.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

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

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, StudentProfileRepository studentProfileRepository, FacultyProfileRepository facultyProfileRepository, CourseRepository courseRepository, SubjectRepository subjectRepository, FacultySubjectAssignmentRepository assignmentRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider, AuditLogService auditLogService) {
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

        auditLogService.logAction(user, "USER_LOGIN", "users", user.getId().toString(), "Successful login", ipAddress);

        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .build();
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
