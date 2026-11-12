package com.virtualclassroom;

import com.virtualclassroom.dto.StudentEnrollmentDto;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.StudentProfileRepository;
import com.virtualclassroom.repository.StudentSubjectEnrollmentRepository;
import com.virtualclassroom.repository.SubjectRepository;
import com.virtualclassroom.service.AuditLogService;
import com.virtualclassroom.service.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private StudentSubjectEnrollmentRepository enrollmentRepository;
    @Mock
    private StudentProfileRepository profileRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private User studentUser;
    private Course csCourse;
    private Course eeCourse;
    private StudentProfile studentProfile;
    private Subject validSubject;
    private Subject diffCourseSubject;
    private Subject diffYearSubject;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(200L)
                .email("student@test.com")
                .fullName("Alice Brown")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.APPROVED)
                .build();

        csCourse = Course.builder().id(1).code("CS").name("Computer Science").build();
        eeCourse = Course.builder().id(2).code("EE").name("Electrical Engineering").build();

        studentProfile = StudentProfile.builder()
                .user(studentUser)
                .course(csCourse)
                .currentYear(2)
                .rollNumber("CS-2024-001")
                .build();

        validSubject = Subject.builder()
                .id(10)
                .title("Data Structures")
                .code("CS201")
                .course(csCourse)
                .yearLevel(2)
                .semester(3)
                .credits(4)
                .build();

        diffCourseSubject = Subject.builder()
                .id(20)
                .title("Circuit Analysis")
                .code("EE201")
                .course(eeCourse)
                .yearLevel(2)
                .semester(3)
                .credits(4)
                .build();

        diffYearSubject = Subject.builder()
                .id(30)
                .title("Cloud Architecture")
                .code("CS401")
                .course(csCourse)
                .yearLevel(4)
                .semester(7)
                .credits(4)
                .build();
    }

    @Test
    @DisplayName("Student cannot enroll in subject belonging to different course")
    void enroll_DifferentCourse_ThrowsIllegalArgumentException() {
        when(profileRepository.findById(200L)).thenReturn(Optional.of(studentProfile));
        when(subjectRepository.findById(20)).thenReturn(Optional.of(diffCourseSubject));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                enrollmentService.enrollStudentInSubject(200L, 20, "2026-FALL", studentUser, "127.0.0.1"));

        assertTrue(ex.getMessage().contains("different degree program"));
    }

    @Test
    @DisplayName("Student cannot enroll in subject of a different year level")
    void enroll_DifferentYear_ThrowsIllegalArgumentException() {
        when(profileRepository.findById(200L)).thenReturn(Optional.of(studentProfile));
        when(subjectRepository.findById(30)).thenReturn(Optional.of(diffYearSubject));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                enrollmentService.enrollStudentInSubject(200L, 30, "2026-FALL", studentUser, "127.0.0.1"));

        assertTrue(ex.getMessage().contains("does not match student's current year"));
    }

    @Test
    @DisplayName("Student cannot duplicate enrollment in same subject and term")
    void enroll_DuplicateEnrollment_ThrowsIllegalArgumentException() {
        when(profileRepository.findById(200L)).thenReturn(Optional.of(studentProfile));
        when(subjectRepository.findById(10)).thenReturn(Optional.of(validSubject));
        when(enrollmentRepository.existsByStudentIdAndSubjectIdAndAcademicTerm(200L, 10, "2026-FALL"))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                enrollmentService.enrollStudentInSubject(200L, 10, "2026-FALL", studentUser, "127.0.0.1"));

        assertTrue(ex.getMessage().contains("already enrolled"));
    }

    @Test
    @DisplayName("Student successfully enrolls in valid subject matching course and year")
    void enroll_ValidSubject_EnrollsSuccessfully() {
        when(profileRepository.findById(200L)).thenReturn(Optional.of(studentProfile));
        when(subjectRepository.findById(10)).thenReturn(Optional.of(validSubject));
        when(enrollmentRepository.existsByStudentIdAndSubjectIdAndAcademicTerm(200L, 10, "2026-FALL"))
                .thenReturn(false);
        when(enrollmentRepository.save(any(StudentSubjectEnrollment.class))).thenAnswer(i -> {
            StudentSubjectEnrollment e = i.getArgument(0);
            e.setId(55L);
            return e;
        });

        StudentEnrollmentDto dto = enrollmentService.enrollStudentInSubject(200L, 10, "2026-FALL", studentUser, "127.0.0.1");

        assertNotNull(dto);
        assertEquals(55L, dto.getId());
        assertEquals("CS201", dto.getSubjectCode());
        assertEquals(EnrollmentStatus.ENROLLED, dto.getStatus());
        verify(enrollmentRepository).save(any(StudentSubjectEnrollment.class));
    }

    @Test
    @DisplayName("Student cannot enroll another student")
    void enroll_AnotherStudent_ThrowsAccessDeniedException() {
        User otherStudent = User.builder().id(300L).role(Role.ROLE_STUDENT).build();

        assertThrows(AccessDeniedException.class, () ->
                enrollmentService.enrollStudentInSubject(200L, 10, "2026-FALL", otherStudent, "127.0.0.1"));
    }
}
