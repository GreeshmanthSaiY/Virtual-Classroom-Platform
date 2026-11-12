package com.virtualclassroom;

import com.virtualclassroom.dto.CreateExaminationRequest;
import com.virtualclassroom.dto.ExaminationDto;
import com.virtualclassroom.dto.ExaminationResultDto;
import com.virtualclassroom.dto.RecordGradeRequest;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.ExaminationRepository;
import com.virtualclassroom.repository.ExaminationResultRepository;
import com.virtualclassroom.repository.FacultySubjectAssignmentRepository;
import com.virtualclassroom.repository.StudentSubjectEnrollmentRepository;
import com.virtualclassroom.repository.SubjectRepository;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.service.AuditLogService;
import com.virtualclassroom.service.ExaminationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExaminationServiceTest {

    @Mock
    private ExaminationRepository examinationRepository;
    @Mock
    private ExaminationResultRepository resultRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private FacultySubjectAssignmentRepository facultyAssignmentRepository;
    @Mock
    private StudentSubjectEnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ExaminationService examinationService;

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
                .title("Operating Systems")
                .code("CS401")
                .build();
    }

    @Test
    @DisplayName("Faculty cannot create examination for unassigned subject")
    void createExamination_UnassignedFaculty_ThrowsAccessDeniedException() {
        CreateExaminationRequest req = new CreateExaminationRequest();
        req.setSubjectId(10);
        req.setTitle("Midterm Exam");
        req.setExamType(ExamType.MIDTERM);
        req.setTotalMarks(100);
        req.setDurationMinutes(120);
        req.setScheduledAt(Instant.now().plusSeconds(86400));

        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(100L, 10)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                examinationService.createExamination(req, facultyUser, "127.0.0.1"));
    }

    @Test
    @DisplayName("Faculty can create examination and record student grade")
    void recordGrade_AssignedFaculty_RecordsSuccessfully() {
        Examination exam = Examination.builder()
                .id(700L)
                .subject(subject)
                .title("Midterm Exam")
                .examType(ExamType.MIDTERM)
                .totalMarks(100)
                .build();

        RecordGradeRequest req = new RecordGradeRequest();
        req.setExaminationId(700L);
        req.setStudentId(200L);
        req.setMarksObtained(BigDecimal.valueOf(88));
        req.setGrade("A");
        req.setRemarks("Great job");

        when(examinationRepository.findById(700L)).thenReturn(Optional.of(exam));
        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(100L, 10)).thenReturn(true);
        when(userRepository.findById(200L)).thenReturn(Optional.of(studentUser));
        when(enrollmentRepository.existsByStudentIdAndSubjectId(200L, 10)).thenReturn(true);
        when(resultRepository.findByExaminationIdAndStudentId(700L, 200L)).thenReturn(Optional.empty());
        when(resultRepository.save(any(ExaminationResult.class))).thenAnswer(i -> {
            ExaminationResult res = i.getArgument(0);
            res.setId(1001L);
            return res;
        });

        ExaminationResultDto result = examinationService.recordGrade(req, facultyUser, "127.0.0.1");

        assertNotNull(result);
        assertEquals(1001L, result.getId());
        assertEquals(BigDecimal.valueOf(88), result.getMarksObtained());
        assertEquals("A", result.getGrade());
    }

    @Test
    @DisplayName("Student cannot view examinations for unenrolled subject")
    void getExaminations_UnenrolledSubject_ThrowsAccessDeniedException() {
        when(enrollmentRepository.existsByStudentIdAndSubjectId(200L, 10)).thenReturn(false);

        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(0, 10);
        assertThrows(AccessDeniedException.class, () ->
                examinationService.getExaminationsForSubjectPaged(10, studentUser, pageRequest));
    }

    @Test
    @DisplayName("Faculty cannot view examination results for unassigned subject")
    void getResults_UnassignedFaculty_ThrowsAccessDeniedException() {
        Examination exam = Examination.builder()
                .id(700L)
                .subject(subject)
                .title("Midterm Exam")
                .build();

        when(examinationRepository.findById(700L)).thenReturn(Optional.of(exam));
        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(100L, 10)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                examinationService.getResultsForExamination(700L, facultyUser));
    }
}
