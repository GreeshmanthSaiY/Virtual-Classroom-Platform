package com.virtualclassroom;

import com.virtualclassroom.dto.AssignmentDto;
import com.virtualclassroom.dto.AssignmentSubmissionDto;
import com.virtualclassroom.dto.CreateAssignmentRequest;
import com.virtualclassroom.dto.GradeSubmissionRequest;
import com.virtualclassroom.dto.SubmitAssignmentRequest;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import com.virtualclassroom.service.AssignmentService;
import com.virtualclassroom.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentSubmissionRepository submissionRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private FacultySubjectAssignmentRepository facultyAssignmentRepository;
    @Mock
    private StudentSubjectEnrollmentRepository enrollmentRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AssignmentService assignmentService;

    private User facultyUser;
    private User studentUser;
    private Subject subject;

    @BeforeEach
    void setUp() {
        facultyUser = User.builder()
                .id(100L)
                .email("faculty@test.com")
                .fullName("Dr. Smith")
                .role(Role.ROLE_FACULTY)
                .status(UserStatus.APPROVED)
                .build();

        studentUser = User.builder()
                .id(200L)
                .email("student@test.com")
                .fullName("Alice Brown")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.APPROVED)
                .build();

        subject = Subject.builder()
                .id(10)
                .title("Data Structures")
                .code("CS201")
                .semester(3)
                .credits(4)
                .build();
    }

    @Test
    @DisplayName("Faculty cannot create assignment for subject not assigned to them")
    void createAssignment_UnassignedFaculty_ThrowsAccessDeniedException() {
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setSubjectId(10);
        req.setTitle("Binary Trees");
        req.setDescription("Implement AVL Tree");
        req.setMaxPoints(100);
        req.setDueDate(Instant.now().plusSeconds(86400));

        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(100L, 10)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                assignmentService.createAssignment(req, facultyUser, "127.0.0.1"));
    }

    @Test
    @DisplayName("Faculty can create assignment for assigned subject")
    void createAssignment_AssignedFaculty_CreatesSuccessfully() {
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setSubjectId(10);
        req.setTitle("Binary Trees");
        req.setDescription("Implement AVL Tree");
        req.setMaxPoints(100);
        req.setDueDate(Instant.now().plusSeconds(86400));

        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(100L, 10)).thenReturn(true);
        when(subjectRepository.findById(10)).thenReturn(Optional.of(subject));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(i -> {
            Assignment a = i.getArgument(0);
            a.setId(500L);
            return a;
        });

        AssignmentDto result = assignmentService.createAssignment(req, facultyUser, "127.0.0.1");

        assertNotNull(result);
        assertEquals(500L, result.getId());
        assertEquals("Binary Trees", result.getTitle());
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    @DisplayName("Student cannot submit assignment if not enrolled in subject")
    void submitAssignment_UnenrolledStudent_ThrowsAccessDeniedException() {
        Assignment assignment = Assignment.builder()
                .id(500L)
                .subject(subject)
                .title("Binary Trees")
                .dueDate(Instant.now().plusSeconds(86400))
                .build();

        SubmitAssignmentRequest req = new SubmitAssignmentRequest();
        req.setAssignmentId(500L);
        req.setFileUrl("https://storage.example.com/sub.pdf");

        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndSubjectId(200L, 10)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                assignmentService.submitAssignment(req, studentUser, "127.0.0.1"));
    }

    @Test
    @DisplayName("Student can submit assignment when actively enrolled")
    void submitAssignment_EnrolledStudent_SubmitsSuccessfully() {
        Assignment assignment = Assignment.builder()
                .id(500L)
                .subject(subject)
                .title("Binary Trees")
                .dueDate(Instant.now().plusSeconds(86400))
                .build();

        SubmitAssignmentRequest req = new SubmitAssignmentRequest();
        req.setAssignmentId(500L);
        req.setFileUrl("https://storage.example.com/sub.pdf");
        req.setSubmissionText("Completed tree traversal");

        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndSubjectId(200L, 10)).thenReturn(true);
        when(submissionRepository.findByAssignmentIdAndStudentId(500L, 200L)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(i -> {
            AssignmentSubmission s = i.getArgument(0);
            s.setId(900L);
            return s;
        });

        AssignmentSubmissionDto sub = assignmentService.submitAssignment(req, studentUser, "127.0.0.1");

        assertNotNull(sub);
        assertEquals(900L, sub.getId());
        assertEquals(SubmissionStatus.SUBMITTED, sub.getStatus());
        verify(submissionRepository).save(any(AssignmentSubmission.class));
    }

    @Test
    @DisplayName("Faculty can grade assignment submission")
    void gradeSubmission_AssignedFaculty_GradesSuccessfully() {
        Assignment assignment = Assignment.builder()
                .id(500L)
                .subject(subject)
                .maxPoints(100)
                .build();

        AssignmentSubmission sub = AssignmentSubmission.builder()
                .id(900L)
                .assignment(assignment)
                .student(studentUser)
                .status(SubmissionStatus.SUBMITTED)
                .build();

        GradeSubmissionRequest req = new GradeSubmissionRequest();
        req.setGradePoints(95);
        req.setFeedback("Excellent implementation");

        when(submissionRepository.findById(900L)).thenReturn(Optional.of(sub));
        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(100L, 10)).thenReturn(true);
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(i -> i.getArgument(0));

        AssignmentSubmissionDto result = assignmentService.gradeSubmission(900L, req, facultyUser, "127.0.0.1");

        assertNotNull(result);
        assertEquals(SubmissionStatus.GRADED, result.getStatus());
        assertEquals(95, result.getGradePoints());
        assertEquals("Excellent implementation", result.getFeedback());
    }

    @Test
    @DisplayName("Student cannot view assignments for unenrolled subject")
    void getAssignments_UnenrolledStudent_ThrowsAccessDeniedException() {
        when(enrollmentRepository.existsByStudentIdAndSubjectId(200L, 10)).thenReturn(false);

        PageRequest pageRequest = PageRequest.of(0, 10);
        assertThrows(AccessDeniedException.class, () ->
                assignmentService.getAssignmentsForSubjectPaged(10, studentUser, pageRequest));
    }
}
