package com.virtualclassroom;

import com.virtualclassroom.dto.CreateLectureRequest;
import com.virtualclassroom.dto.CreateMaterialRequest;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacultyAuthorizationSecurityTest {

    @Mock
    private FacultySubjectAssignmentRepository assignmentRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private StudyMaterialRepository studyMaterialRepository;
    @Mock
    private VideoLectureRepository videoLectureRepository;
    @Mock
    private CourseService courseService;
    @Mock
    private AuditLogService auditLogService;

    private FacultyService facultyService;

    private User facultyUser;
    private User adminUser;
    private Subject mathSubject;

    @BeforeEach
    void setUp() {
        facultyService = new FacultyService(
                assignmentRepository,
                subjectRepository,
                studyMaterialRepository,
                videoLectureRepository,
                courseService,
                auditLogService
        );

        facultyUser = User.builder()
                .id(2L)
                .email("faculty@test.com")
                .fullName("Dr. Test Faculty")
                .role(Role.ROLE_FACULTY)
                .status(UserStatus.APPROVED)
                .build();

        adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .fullName("Admin")
                .role(Role.ROLE_ADMIN)
                .status(UserStatus.APPROVED)
                .build();

        mathSubject = Subject.builder()
                .id(10)
                .code("MTH101")
                .title("Calculus I")
                .build();
    }

    @Test
    @DisplayName("Faculty cannot upload study material to an unassigned subject")
    void uploadMaterial_UnassignedSubject_ThrowsAccessDenied() {
        CreateMaterialRequest request = new CreateMaterialRequest();
        request.setSubjectId(10);
        request.setTitle("Unauthorized Notes");
        request.setFileUrl("https://example.com/notes.pdf");

        when(assignmentRepository.existsByFacultyIdAndSubjectId(2L, 10)).thenReturn(false);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                facultyService.uploadStudyMaterial(request, facultyUser, "127.0.0.1"));

        assertTrue(ex.getMessage().contains("not assigned to instruct this subject"));
        verify(studyMaterialRepository, never()).save(any());
    }

    @Test
    @DisplayName("Faculty can upload study material to an assigned subject")
    void uploadMaterial_AssignedSubject_Succeeds() {
        CreateMaterialRequest request = new CreateMaterialRequest();
        request.setSubjectId(10);
        request.setTitle("Valid Lecture Notes");
        request.setFileUrl("https://example.com/notes.pdf");

        when(assignmentRepository.existsByFacultyIdAndSubjectId(2L, 10)).thenReturn(true);
        when(subjectRepository.findById(10)).thenReturn(Optional.of(mathSubject));
        when(studyMaterialRepository.save(any(StudyMaterial.class))).thenAnswer(invocation -> {
            StudyMaterial sm = invocation.getArgument(0);
            sm.setId(50L);
            return sm;
        });

        assertDoesNotThrow(() ->
                facultyService.uploadStudyMaterial(request, facultyUser, "127.0.0.1"));
        verify(studyMaterialRepository, times(1)).save(any(StudyMaterial.class));
    }

    @Test
    @DisplayName("Faculty cannot publish video lecture to an unassigned subject")
    void createLecture_UnassignedSubject_ThrowsAccessDenied() {
        CreateLectureRequest request = new CreateLectureRequest();
        request.setSubjectId(10);
        request.setTitle("Unauthorized Video");
        request.setVideoUrl("https://youtube.com/watch?v=123");

        when(assignmentRepository.existsByFacultyIdAndSubjectId(2L, 10)).thenReturn(false);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                facultyService.createVideoLecture(request, facultyUser, "127.0.0.1"));

        assertTrue(ex.getMessage().contains("not assigned to instruct this subject"));
        verify(videoLectureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin can publish video lecture without subject assignment check")
    void createLecture_AdminUser_Succeeds() {
        CreateLectureRequest request = new CreateLectureRequest();
        request.setSubjectId(10);
        request.setTitle("Admin Keynote");
        request.setVideoUrl("https://youtube.com/watch?v=admin");

        when(subjectRepository.findById(10)).thenReturn(Optional.of(mathSubject));
        when(videoLectureRepository.save(any(VideoLecture.class))).thenAnswer(invocation -> {
            VideoLecture vl = invocation.getArgument(0);
            vl.setId(75L);
            return vl;
        });

        assertDoesNotThrow(() ->
                facultyService.createVideoLecture(request, adminUser, "127.0.0.1"));
        verify(assignmentRepository, never()).existsByFacultyIdAndSubjectId(anyLong(), anyInt());
        verify(videoLectureRepository, times(1)).save(any(VideoLecture.class));
    }
}

