package com.virtualclassroom;

import com.virtualclassroom.dto.StudentDashboardDto;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import com.virtualclassroom.service.CourseService;
import com.virtualclassroom.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentDashboardTest {

    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private StudyMaterialRepository studyMaterialRepository;
    @Mock private VideoLectureRepository videoLectureRepository;
    @Mock private CourseService courseService;
    @Mock private LiveSessionRepository liveSessionRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private ExaminationResultRepository examResultRepository;
    @Mock private NotificationRepository notificationRepository;

    private StudentService studentService;
    private User student;
    private Course course;
    private Subject subject;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(
                studentProfileRepository, subjectRepository, studyMaterialRepository,
                videoLectureRepository, courseService, liveSessionRepository,
                assignmentRepository, submissionRepository, examResultRepository, notificationRepository
        );

        student = User.builder()
                .id(20L)
                .email("student@university.edu")
                .fullName("Alice Student")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.APPROVED)
                .build();

        course = Course.builder()
                .id(1)
                .code("BSC-CS")
                .name("Bachelor of Science in Computer Science")
                .build();

        subject = Subject.builder()
                .id(101)
                .code("CS101")
                .title("Introduction to Algorithms")
                .course(course)
                .yearLevel(1)
                .build();
    }

    @Test
    @DisplayName("Student dashboard aggregates enrolled subjects, upcoming sessions, pending assignments, and notification count")
    void getStudentDashboard_ReturnsAggregatedData() {
        StudentProfile profile = StudentProfile.builder()
                .user(student)
                .rollNumber("CS-2026-001")
                .course(course)
                .currentYear(1)
                .build();

        when(studentProfileRepository.findById(20L)).thenReturn(Optional.of(profile));
        when(subjectRepository.findByCourseIdAndYearLevel(1, 1)).thenReturn(List.of(subject));

        LiveSession session = LiveSession.builder()
                .id(1L)
                .subject(subject)
                .host(User.builder().id(2L).fullName("Prof. Oak").build())
                .title("Algorithms Live Recitation")
                .meetingLink("https://meet.jit.si/alg-101")
                .platform("JITSI")
                .scheduledStart(Instant.now().plus(2, ChronoUnit.HOURS))
                .scheduledEnd(Instant.now().plus(3, ChronoUnit.HOURS))
                .status(LiveSessionStatus.SCHEDULED)
                .build();

        when(liveSessionRepository.findBySubjectIdInAndStatusOrderByScheduledStartAsc(List.of(101), LiveSessionStatus.SCHEDULED))
                .thenReturn(List.of(session));

        Assignment assignment = Assignment.builder()
                .id(10L)
                .subject(subject)
                .faculty(User.builder().id(2L).fullName("Prof. Oak").build())
                .title("HW1: Recursion")
                .description("Solve problems 1-5")
                .dueDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .maxPoints(100)
                .build();

        when(assignmentRepository.findBySubjectIdInOrderByDueDateAsc(List.of(101)))
                .thenReturn(List.of(assignment));
        when(submissionRepository.existsByAssignmentIdAndStudentId(10L, 20L)).thenReturn(false);

        when(notificationRepository.countByUserIdAndIsReadFalse(20L)).thenReturn(3L);

        StudentDashboardDto dashboard = studentService.getStudentDashboard(20L);

        assertNotNull(dashboard);
        assertEquals(1, dashboard.getEnrolledSubjects().size());
        assertEquals(1, dashboard.getUpcomingLiveSessions().size());
        assertEquals("Algorithms Live Recitation", dashboard.getUpcomingLiveSessions().get(0).getTitle());
        assertEquals(1, dashboard.getPendingAssignments().size());
        assertEquals("HW1: Recursion", dashboard.getPendingAssignments().get(0).getTitle());
        assertEquals(3L, dashboard.getUnreadNotificationCount());
    }
}
