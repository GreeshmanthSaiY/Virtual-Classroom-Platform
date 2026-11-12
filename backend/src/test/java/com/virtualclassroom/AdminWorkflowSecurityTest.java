package com.virtualclassroom;

import com.virtualclassroom.dto.PlatformMetricsDto;
import com.virtualclassroom.dto.UserDto;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import com.virtualclassroom.service.AdminService;
import com.virtualclassroom.service.AuditLogService;
import com.virtualclassroom.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminWorkflowSecurityTest {

    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private StudyMaterialRepository studyMaterialRepository;
    @Mock private VideoLectureRepository videoLectureRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private FacultyProfileRepository facultyProfileRepository;
    @Mock private FacultySubjectAssignmentRepository facultySubjectAssignmentRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private ExaminationRepository examinationRepository;
    @Mock private LiveSessionRepository liveSessionRepository;

    private AdminService adminService;
    private User adminActor;
    private User targetStudent;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                userRepository, courseRepository, subjectRepository, studyMaterialRepository,
                videoLectureRepository, questionRepository, notificationService, auditLogService,
                refreshTokenRepository, facultyProfileRepository, facultySubjectAssignmentRepository,
                assignmentRepository, examinationRepository, liveSessionRepository
        );

        adminActor = User.builder()
                .id(1L)
                .email("admin@university.edu")
                .role(Role.ROLE_ADMIN)
                .status(UserStatus.APPROVED)
                .build();

        targetStudent = User.builder()
                .id(10L)
                .email("student@university.edu")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.APPROVED)
                .build();
    }

    @Test
    @DisplayName("Admin suspends user: sets SUSPENDED, invalidates all sessions, notifies user and logs audit action")
    void suspendUser_SuccessfulSuspension() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(targetStudent));

        adminService.suspendUser(10L, "Code of conduct violation", adminActor, "192.168.1.1");

        assertEquals(UserStatus.SUSPENDED, targetStudent.getStatus());
        assertEquals("Code of conduct violation", targetStudent.getRejectionReason());
        verify(userRepository).save(targetStudent);
        verify(refreshTokenRepository).revokeAllActiveByUserId(10L);
        verify(notificationService).createNotification(eq(targetStudent), eq("Account Suspended"), anyString(), eq("ACCOUNT_SUSPENDED"));
        verify(auditLogService).logAction(eq(adminActor), eq("SUSPEND_USER"), eq("users"), eq("10"), anyString(), eq("192.168.1.1"));
    }

    @Test
    @DisplayName("Admin cannot suspend their own account")
    void suspendUser_SelfSuspensionBlocked() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adminService.suspendUser(1L, "Accidental self-lock", adminActor, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("cannot suspend their own account"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin reactivates suspended user: restores APPROVED status and notifies user")
    void reactivateUser_SuccessfulReactivation() {
        targetStudent.setStatus(UserStatus.SUSPENDED);
        targetStudent.setRejectionReason("Prior reason");
        when(userRepository.findById(10L)).thenReturn(Optional.of(targetStudent));

        adminService.reactivateUser(10L, adminActor, "192.168.1.1");

        assertEquals(UserStatus.APPROVED, targetStudent.getStatus());
        assertNull(targetStudent.getRejectionReason());
        verify(userRepository).save(targetStudent);
        verify(notificationService).createNotification(eq(targetStudent), eq("Account Reactivated"), anyString(), eq("ACCOUNT_REACTIVATED"));
        verify(auditLogService).logAction(eq(adminActor), eq("REACTIVATE_USER"), eq("users"), eq("10"), anyString(), eq("192.168.1.1"));
    }

    @Test
    @DisplayName("Admin search users by query, role, and status")
    void searchUsers_FiltersCorrectly() {
        Page<User> mockPage = new PageImpl<>(List.of(targetStudent));
        when(userRepository.searchUsers("student", Role.ROLE_STUDENT, UserStatus.APPROVED, PageRequest.of(0, 10)))
                .thenReturn(mockPage);

        Page<UserDto> result = adminService.searchUsers("student", Role.ROLE_STUDENT, UserStatus.APPROVED, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("student@university.edu", result.getContent().get(0).getEmail());
    }

    @Test
    @DisplayName("Admin platform metrics aggregates comprehensive platform statistics")
    void getPlatformMetrics_CalculatesAllPlatformStats() {
        when(userRepository.countByRole(Role.ROLE_STUDENT)).thenReturn(150L);
        when(userRepository.countByRole(Role.ROLE_FACULTY)).thenReturn(25L);
        when(userRepository.countByRoleAndStatus(Role.ROLE_STUDENT, UserStatus.PENDING)).thenReturn(5L);
        when(userRepository.countByRoleAndStatus(Role.ROLE_FACULTY, UserStatus.PENDING)).thenReturn(2L);
        when(courseRepository.count()).thenReturn(10L);
        when(subjectRepository.count()).thenReturn(40L);
        when(studyMaterialRepository.count()).thenReturn(85L);
        when(videoLectureRepository.count()).thenReturn(30L);
        when(questionRepository.count()).thenReturn(60L);
        when(questionRepository.countByIsResolvedTrue()).thenReturn(45L);
        when(assignmentRepository.count()).thenReturn(20L);
        when(examinationRepository.count()).thenReturn(8L);
        when(liveSessionRepository.countByStatus(LiveSessionStatus.SCHEDULED)).thenReturn(3L);
        when(userRepository.countByStatus(UserStatus.SUSPENDED)).thenReturn(1L);
        when(userRepository.countByStatus(UserStatus.APPROVED)).thenReturn(170L);

        PlatformMetricsDto metrics = adminService.getPlatformMetrics();

        assertNotNull(metrics);
        assertEquals(150L, metrics.getTotalStudents());
        assertEquals(25L, metrics.getTotalFaculty());
        assertEquals(5L, metrics.getPendingStudentApprovals());
        assertEquals(20L, metrics.getTotalAssignments());
        assertEquals(8L, metrics.getTotalExaminations());
        assertEquals(3L, metrics.getUpcomingLiveSessions());
        assertEquals(1L, metrics.getSuspendedUsers());
        assertEquals(170L, metrics.getApprovedUsers());
    }
}
