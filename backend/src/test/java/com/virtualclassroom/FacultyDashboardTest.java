package com.virtualclassroom;

import com.virtualclassroom.dto.FacultyDashboardDto;
import com.virtualclassroom.dto.SubjectDto;
import com.virtualclassroom.dto.UserDto;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import com.virtualclassroom.service.AuditLogService;
import com.virtualclassroom.service.CourseService;
import com.virtualclassroom.service.FacultyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacultyDashboardTest {

    @Mock private FacultySubjectAssignmentRepository assignmentRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private StudyMaterialRepository studyMaterialRepository;
    @Mock private VideoLectureRepository videoLectureRepository;
    @Mock private CourseService courseService;
    @Mock private AuditLogService auditLogService;
    @Mock private LiveSessionRepository liveSessionRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private StudentSubjectEnrollmentRepository studentEnrollmentRepository;

    private FacultyService facultyService;
    private User facultyUser;
    private Subject subject;

    @BeforeEach
    void setUp() {
        facultyService = new FacultyService(
                assignmentRepository, subjectRepository, studyMaterialRepository,
                videoLectureRepository, courseService, auditLogService,
                liveSessionRepository, submissionRepository, questionRepository, studentEnrollmentRepository
        );

        facultyUser = User.builder()
                .id(5L)
                .email("dr.turing@university.edu")
                .fullName("Dr. Alan Turing")
                .role(Role.ROLE_FACULTY)
                .status(UserStatus.APPROVED)
                .build();

        subject = Subject.builder()
                .id(202)
                .code("CS202")
                .title("Theory of Computation")
                .yearLevel(2)
                .build();
    }

    @Test
    @DisplayName("Faculty dashboard aggregates assigned subjects, counts, pending submissions, and recent forum questions")
    void getFacultyDashboard_ReturnsAggregatedData() {
        FacultySubjectAssignment fsa = FacultySubjectAssignment.builder()
                .facultyId(5L)
                .subjectId(202)
                .subject(subject)
                .academicTerm("2026-ACTIVE")
                .build();

        when(assignmentRepository.findByFacultyId(5L)).thenReturn(List.of(fsa));
        when(courseService.toSubjectDto(subject)).thenReturn(
                SubjectDto.builder().id(202).code("CS202").title("Theory of Computation").yearLevel(2).build()
        );

        when(studyMaterialRepository.countByUploaderId(5L)).thenReturn(12L);
        when(videoLectureRepository.countByFacultyId(5L)).thenReturn(8L);

        AssignmentSubmission pendingSubmission = AssignmentSubmission.builder()
                .id(1L)
                .assignment(Assignment.builder().id(10L).title("Turing Machines HW").maxPoints(100).build())
                .student(User.builder().id(30L).fullName("Bob Student").build())
                .submittedAt(Instant.now())
                .status(SubmissionStatus.SUBMITTED)
                .build();

        when(submissionRepository.findPendingGradingByFacultyId(5L)).thenReturn(List.of(pendingSubmission));
        when(submissionRepository.countPendingGradingByFacultyId(5L)).thenReturn(1L);

        Question forumQ = Question.builder()
                .id(100L)
                .subject(subject)
                .author(User.builder().id(30L).fullName("Bob Student").role(Role.ROLE_STUDENT).build())
                .title("Halting problem clarification")
                .body("Is the halting problem always undecidable?")
                .createdAt(Instant.now())
                .build();

        when(questionRepository.findBySubjectIdOrderByCreatedAtDesc(202)).thenReturn(List.of(forumQ));

        FacultyDashboardDto dashboard = facultyService.getFacultyDashboard(5L);

        assertNotNull(dashboard);
        assertEquals(1, dashboard.getAssignedSubjects().size());
        assertEquals("CS202", dashboard.getAssignedSubjects().get(0).getCode());
        assertEquals(12L, dashboard.getTotalMaterialsUploaded());
        assertEquals(8L, dashboard.getTotalLecturesPublished());
        assertEquals(1, dashboard.getPendingSubmissionsToGrade().size());
        assertEquals(1L, dashboard.getPendingSubmissionsCount());
        assertEquals(1, dashboard.getRecentForumQuestions().size());
        assertEquals("Halting problem clarification", dashboard.getRecentForumQuestions().get(0).getTitle());
    }

    @Test
    @DisplayName("Faculty roster inspection: assigned instructor can view enrolled students")
    void getEnrolledStudentsForSubject_AuthorizedFaculty_ReturnsRoster() {
        when(assignmentRepository.existsByFacultyIdAndSubjectId(5L, 202)).thenReturn(true);

        User student = User.builder()
                .id(30L)
                .email("bob@university.edu")
                .fullName("Bob Student")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.APPROVED)
                .build();

        StudentSubjectEnrollment enrollment = StudentSubjectEnrollment.builder()
                .student(student)
                .subject(subject)
                .academicTerm("2026-ACTIVE")
                .status(EnrollmentStatus.ENROLLED)
                .build();

        when(studentEnrollmentRepository.findBySubjectId(202)).thenReturn(List.of(enrollment));

        List<UserDto> roster = facultyService.getEnrolledStudentsForSubject(202, 5L);

        assertNotNull(roster);
        assertEquals(1, roster.size());
        assertEquals("bob@university.edu", roster.get(0).getEmail());
    }

    @Test
    @DisplayName("Faculty roster inspection: unassigned instructor is denied access")
    void getEnrolledStudentsForSubject_UnassignedFaculty_ThrowsAccessDenied() {
        when(assignmentRepository.existsByFacultyIdAndSubjectId(5L, 999)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                facultyService.getEnrolledStudentsForSubject(999, 5L)
        );
    }
}
