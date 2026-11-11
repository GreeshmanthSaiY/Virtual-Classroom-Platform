package com.virtualclassroom;

import com.virtualclassroom.dto.AuthResponse;
import com.virtualclassroom.dto.LoginRequest;
import com.virtualclassroom.dto.RegisterStudentRequest;
import com.virtualclassroom.dto.UserDto;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import com.virtualclassroom.security.JwtTokenProvider;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.AuditLogService;
import com.virtualclassroom.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private FacultyProfileRepository facultyProfileRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private FacultySubjectAssignmentRepository assignmentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogService auditLogService;

    private JwtTokenProvider tokenProvider;
    private AuthService authService;

    private User approvedStudent;
    private User pendingStudent;
    private User rejectedStudent;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 86400000L);
        authService = new AuthService(
                authenticationManager,
                userRepository,
                studentProfileRepository,
                facultyProfileRepository,
                courseRepository,
                subjectRepository,
                assignmentRepository,
                passwordEncoder,
                tokenProvider,
                auditLogService
        );

        approvedStudent = User.builder()
                .id(1L)
                .email("student@test.com")
                .passwordHash("hashedPass")
                .fullName("John Doe")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.APPROVED)
                .build();

        pendingStudent = User.builder()
                .id(2L)
                .email("pending@test.com")
                .passwordHash("hashedPass")
                .fullName("Jane Doe")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.PENDING)
                .build();

        rejectedStudent = User.builder()
                .id(3L)
                .email("rejected@test.com")
                .passwordHash("hashedPass")
                .fullName("Bob Smith")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.REJECTED)
                .rejectionReason("Duplicate registration")
                .build();
    }

    @Test
    @DisplayName("Login succeeds for verified approved user")
    void login_ApprovedUser_ReturnsAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@test.com");
        request.setPassword("Password123!");

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(approvedStudent));
        Authentication auth = mock(Authentication.class);
        UserPrincipal principal = UserPrincipal.create(approvedStudent);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        AuthResponse response = authService.login(request, "127.0.0.1");

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isEmpty());
        assertEquals("student@test.com", response.getEmail());
        assertEquals("ROLE_STUDENT", response.getRole());
        assertEquals("APPROVED", response.getStatus());
        verify(auditLogService, times(1)).logAction(any(), eq("USER_LOGIN"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Login blocked for pending user awaiting admin approval")
    void login_PendingUser_ThrowsIllegalStateException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("pending@test.com");
        request.setPassword("Password123!");

        when(userRepository.findByEmail("pending@test.com")).thenReturn(Optional.of(pendingStudent));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                authService.login(request, "127.0.0.1"));

        assertTrue(ex.getMessage().contains("awaiting Administrator approval"));
    }

    @Test
    @DisplayName("Login blocked for rejected user with documented reason")
    void login_RejectedUser_ThrowsIllegalStateExceptionWithReason() {
        LoginRequest request = new LoginRequest();
        request.setEmail("rejected@test.com");
        request.setPassword("Password123!");

        when(userRepository.findByEmail("rejected@test.com")).thenReturn(Optional.of(rejectedStudent));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                authService.login(request, "127.0.0.1"));

        assertTrue(ex.getMessage().contains("Duplicate registration"));
    }

    @Test
    @DisplayName("Student registration creates user with PENDING status")
    void registerStudent_ValidDetails_CreatesPendingUser() {
        RegisterStudentRequest request = new RegisterStudentRequest();
        request.setEmail("new@student.com");
        request.setPassword("Pass123456!");
        request.setFullName("New Student");
        request.setRollNumber("ROLL-999");
        request.setCourseId(1);
        request.setYear(1);

        Course mockCourse = Course.builder().id(1).name("B.Sc CS").build();
        when(userRepository.existsByEmail("new@student.com")).thenReturn(false);
        when(studentProfileRepository.existsByRollNumber("ROLL-999")).thenReturn(false);
        when(courseRepository.findById(1)).thenReturn(Optional.of(mockCourse));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        UserDto result = authService.registerStudent(request, "127.0.0.1");

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals("ROLE_STUDENT", result.getRole());
        assertEquals("ROLL-999", result.getRollNumber());
        verify(studentProfileRepository, times(1)).save(any(StudentProfile.class));
    }
}
