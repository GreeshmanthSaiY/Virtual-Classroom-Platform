package com.virtualclassroom;

import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.StudentProfileRepository;
import com.virtualclassroom.repository.StudyMaterialRepository;
import com.virtualclassroom.repository.SubjectRepository;
import com.virtualclassroom.repository.VideoLectureRepository;
import com.virtualclassroom.service.CourseService;
import com.virtualclassroom.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentAuthorizationSecurityTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private StudyMaterialRepository studyMaterialRepository;
    @Mock
    private VideoLectureRepository videoLectureRepository;
    @Mock
    private CourseService courseService;

    private StudentService studentService;

    private Course bscCs;
    private Course bscDs;
    private Subject csSubject;
    private Subject dsSubject;
    private StudentProfile csStudentProfile;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(
                studentProfileRepository,
                subjectRepository,
                studyMaterialRepository,
                videoLectureRepository,
                courseService
        );

        bscCs = Course.builder().id(1).code("BSC-CS").name("Computer Science").build();
        bscDs = Course.builder().id(2).code("BSC-DS").name("Data Science").build();

        csSubject = Subject.builder()
                .id(10)
                .code("CS101")
                .course(bscCs)
                .yearLevel(1)
                .build();

        dsSubject = Subject.builder()
                .id(20)
                .code("DS101")
                .course(bscDs)
                .yearLevel(1)
                .build();

        User studentUser = User.builder().id(3L).role(Role.ROLE_STUDENT).build();
        csStudentProfile = StudentProfile.builder()
                .user(studentUser)
                .course(bscCs)
                .currentYear(1)
                .build();
    }

    @Test
    @DisplayName("Student access blocked for subjects outside their enrolled course")
    void verifyStudentEnrolled_DifferentCourse_ThrowsAccessDenied() {
        when(studentProfileRepository.findById(3L)).thenReturn(Optional.of(csStudentProfile));
        when(subjectRepository.findById(20)).thenReturn(Optional.of(dsSubject));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                studentService.verifyStudentEnrolledInSubject(3L, 20));

        assertTrue(ex.getMessage().contains("not enrolled in this subject"));
    }

    @Test
    @DisplayName("Student access permitted for subjects matching their course and year level")
    void verifyStudentEnrolled_MatchingCourseAndYear_Succeeds() {
        when(studentProfileRepository.findById(3L)).thenReturn(Optional.of(csStudentProfile));
        when(subjectRepository.findById(10)).thenReturn(Optional.of(csSubject));

        assertDoesNotThrow(() ->
                studentService.verifyStudentEnrolledInSubject(3L, 10));
    }

    @Test
    @DisplayName("Student access blocked if subject year level does not match current year")
    void verifyStudentEnrolled_DifferentYear_ThrowsAccessDenied() {
        Subject seniorSubject = Subject.builder()
                .id(30)
                .code("CS301")
                .course(bscCs)
                .yearLevel(3)
                .build();

        when(studentProfileRepository.findById(3L)).thenReturn(Optional.of(csStudentProfile));
        when(subjectRepository.findById(30)).thenReturn(Optional.of(seniorSubject));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                studentService.verifyStudentEnrolledInSubject(3L, 30));

        assertTrue(ex.getMessage().contains("not enrolled in this subject"));
    }
}
