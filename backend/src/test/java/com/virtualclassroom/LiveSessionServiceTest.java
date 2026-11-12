package com.virtualclassroom;

import com.virtualclassroom.dto.CreateLiveSessionRequest;
import com.virtualclassroom.dto.LiveSessionDto;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.FacultySubjectAssignmentRepository;
import com.virtualclassroom.repository.LiveSessionRepository;
import com.virtualclassroom.repository.StudentSubjectEnrollmentRepository;
import com.virtualclassroom.repository.SubjectRepository;
import com.virtualclassroom.service.AuditLogService;
import com.virtualclassroom.service.LiveSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveSessionServiceTest {

    @Mock
    private LiveSessionRepository liveSessionRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private FacultySubjectAssignmentRepository facultyAssignmentRepository;
    @Mock
    private StudentSubjectEnrollmentRepository enrollmentRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private LiveSessionService liveSessionService;

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
                .title("Database Systems")
                .code("CS301")
                .build();
    }

    @Test
    @DisplayName("Faculty cannot schedule live session for unassigned subject")
    void scheduleSession_UnassignedFaculty_ThrowsAccessDeniedException() {
        CreateLiveSessionRequest req = new CreateLiveSessionRequest();
        req.setSubjectId(10);
        req.setTitle("Intro to Indexing");
        req.setScheduledStart(Instant.now().plusSeconds(3600));
        req.setScheduledEnd(Instant.now().plusSeconds(7200));

        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(100L, 10)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                liveSessionService.scheduleLiveSession(req, facultyUser, "127.0.0.1"));
    }

    @Test
    @DisplayName("Faculty can schedule live session for assigned subject")
    void scheduleSession_AssignedFaculty_SchedulesSuccessfully() {
        CreateLiveSessionRequest req = new CreateLiveSessionRequest();
        req.setSubjectId(10);
        req.setTitle("Intro to Indexing");
        req.setMeetingLink("https://meet.jit.si/vc-cs301-indexing");
        req.setPlatform("Jitsi Meet");
        req.setScheduledStart(Instant.now().plusSeconds(3600));
        req.setScheduledEnd(Instant.now().plusSeconds(7200));

        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(100L, 10)).thenReturn(true);
        when(subjectRepository.findById(10)).thenReturn(Optional.of(subject));
        when(liveSessionRepository.save(any(LiveSession.class))).thenAnswer(i -> {
            LiveSession s = i.getArgument(0);
            s.setId(400L);
            return s;
        });

        LiveSessionDto dto = liveSessionService.scheduleLiveSession(req, facultyUser, "127.0.0.1");

        assertNotNull(dto);
        assertEquals(400L, dto.getId());
        assertEquals(LiveSessionStatus.SCHEDULED, dto.getStatus());
        verify(liveSessionRepository).save(any(LiveSession.class));
    }

    @Test
    @DisplayName("Host faculty can update session status to LIVE and COMPLETED")
    void updateSessionStatus_HostFaculty_UpdatesSuccessfully() {
        LiveSession session = LiveSession.builder()
                .id(400L)
                .subject(subject)
                .host(facultyUser)
                .title("Intro to Indexing")
                .status(LiveSessionStatus.SCHEDULED)
                .build();

        when(liveSessionRepository.findById(400L)).thenReturn(Optional.of(session));
        when(liveSessionRepository.save(any(LiveSession.class))).thenAnswer(i -> i.getArgument(0));

        LiveSessionDto liveDto = liveSessionService.updateSessionStatus(400L, LiveSessionStatus.LIVE, facultyUser, "127.0.0.1");
        assertEquals(LiveSessionStatus.LIVE, liveDto.getStatus());

        LiveSessionDto completedDto = liveSessionService.updateSessionStatus(400L, LiveSessionStatus.COMPLETED, facultyUser, "127.0.0.1");
        assertEquals(LiveSessionStatus.COMPLETED, completedDto.getStatus());
    }

    @Test
    @DisplayName("Scheduling session with end time before start time throws IllegalArgumentException")
    void scheduleSession_EndTimeBeforeStartTime_ThrowsIllegalArgumentException() {
        CreateLiveSessionRequest req = new CreateLiveSessionRequest();
        req.setSubjectId(10);
        req.setTitle("Intro to Indexing");
        Instant now = Instant.now().plusSeconds(3600);
        req.setScheduledStart(now);
        req.setScheduledEnd(now.minusSeconds(1800)); // end is before start

        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(100L, 10)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                liveSessionService.scheduleLiveSession(req, facultyUser, "127.0.0.1"));
    }

    @Test
    @DisplayName("Scheduling session that overlaps with existing active session throws IllegalArgumentException")
    void scheduleSession_OverlappingSession_ThrowsIllegalArgumentException() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        CreateLiveSessionRequest req = new CreateLiveSessionRequest();
        req.setSubjectId(10);
        req.setTitle("Duplicate Session");
        req.setScheduledStart(start);
        req.setScheduledEnd(end);

        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(100L, 10)).thenReturn(true);
        when(liveSessionRepository.existsOverlappingSession(10, start, end)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                liveSessionService.scheduleLiveSession(req, facultyUser, "127.0.0.1"));

        assertTrue(ex.getMessage().contains("overlapping"));
    }

    @Test
    @DisplayName("Attempting to alter a COMPLETED session throws IllegalStateException")
    void updateSessionStatus_CompletedSession_ThrowsIllegalStateException() {
        LiveSession session = LiveSession.builder()
                .id(400L)
                .subject(subject)
                .host(facultyUser)
                .title("Intro to Indexing")
                .status(LiveSessionStatus.COMPLETED)
                .build();

        when(liveSessionRepository.findById(400L)).thenReturn(Optional.of(session));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                liveSessionService.updateSessionStatus(400L, LiveSessionStatus.LIVE, facultyUser, "127.0.0.1"));

        assertTrue(ex.getMessage().contains("already COMPLETED"));
    }

    @Test
    @DisplayName("Invalid state transition from SCHEDULED to COMPLETED throws IllegalStateException")
    void updateSessionStatus_InvalidTransition_ThrowsIllegalStateException() {
        LiveSession session = LiveSession.builder()
                .id(400L)
                .subject(subject)
                .host(facultyUser)
                .title("Intro to Indexing")
                .status(LiveSessionStatus.SCHEDULED)
                .build();

        when(liveSessionRepository.findById(400L)).thenReturn(Optional.of(session));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                liveSessionService.updateSessionStatus(400L, LiveSessionStatus.COMPLETED, facultyUser, "127.0.0.1"));

        assertTrue(ex.getMessage().contains("Invalid status transition"));
    }

    @Test
    @DisplayName("Student cannot view sessions for unenrolled subject")
    void getSessionsForSubject_UnenrolledStudent_ThrowsAccessDeniedException() {
        when(enrollmentRepository.existsByStudentIdAndSubjectId(200L, 10)).thenReturn(false);

        PageRequest pageRequest = PageRequest.of(0, 10);
        assertThrows(AccessDeniedException.class, () ->
                liveSessionService.getSessionsForSubjectPaged(10, studentUser, pageRequest));
    }
}
