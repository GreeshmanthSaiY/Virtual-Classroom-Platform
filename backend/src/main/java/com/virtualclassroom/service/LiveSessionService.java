package com.virtualclassroom.service;

import com.virtualclassroom.dto.CreateLiveSessionRequest;
import com.virtualclassroom.dto.LiveSessionDto;
import com.virtualclassroom.model.LiveSession;
import com.virtualclassroom.model.LiveSessionStatus;
import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.Subject;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.FacultySubjectAssignmentRepository;
import com.virtualclassroom.repository.LiveSessionRepository;
import com.virtualclassroom.repository.StudentSubjectEnrollmentRepository;
import com.virtualclassroom.repository.SubjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LiveSessionService {

    private final LiveSessionRepository liveSessionRepository;
    private final SubjectRepository subjectRepository;
    private final FacultySubjectAssignmentRepository facultyAssignmentRepository;
    private final StudentSubjectEnrollmentRepository enrollmentRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public LiveSessionService(
            LiveSessionRepository liveSessionRepository,
            SubjectRepository subjectRepository,
            FacultySubjectAssignmentRepository facultyAssignmentRepository,
            StudentSubjectEnrollmentRepository enrollmentRepository,
            AuditLogService auditLogService
    ) {
        this(liveSessionRepository, subjectRepository, facultyAssignmentRepository, enrollmentRepository, auditLogService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public LiveSessionService(
            LiveSessionRepository liveSessionRepository,
            SubjectRepository subjectRepository,
            FacultySubjectAssignmentRepository facultyAssignmentRepository,
            StudentSubjectEnrollmentRepository enrollmentRepository,
            AuditLogService auditLogService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) NotificationService notificationService
    ) {
        this.liveSessionRepository = liveSessionRepository;
        this.subjectRepository = subjectRepository;
        this.facultyAssignmentRepository = facultyAssignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    public LiveSessionDto scheduleLiveSession(CreateLiveSessionRequest request, User hostUser, String ipAddress) {
        if (hostUser.getRole() != Role.ROLE_ADMIN) {
            boolean assigned = facultyAssignmentRepository.existsByFacultyIdAndSubjectId(hostUser.getId(), request.getSubjectId());
            if (!assigned) {
                throw new AccessDeniedException("Access denied: You are not assigned to instruct this subject");
            }
        }

        if (request.getScheduledStart() == null || request.getScheduledEnd() == null) {
            throw new IllegalArgumentException("Session start and end times are required");
        }
        if (!request.getScheduledEnd().isAfter(request.getScheduledStart())) {
            throw new IllegalArgumentException("Session end time must be strictly after start time");
        }

        if (liveSessionRepository.existsOverlappingSession(request.getSubjectId(), request.getScheduledStart(), request.getScheduledEnd())) {
            throw new IllegalArgumentException("An active overlapping live session is already scheduled for this subject");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found with ID: " + request.getSubjectId()));

        LiveSession session = LiveSession.builder()
                .subject(subject)
                .host(hostUser)
                .title(request.getTitle())
                .description(request.getDescription())
                .meetingLink(request.getMeetingLink())
                .platform(request.getPlatform())
                .scheduledStart(request.getScheduledStart())
                .scheduledEnd(request.getScheduledEnd())
                .status(LiveSessionStatus.SCHEDULED)
                .build();

        LiveSession saved = liveSessionRepository.save(session);
        auditLogService.logAction(hostUser, "SCHEDULE_LIVE_SESSION", "LiveSession", saved.getId().toString(),
                "Title: " + saved.getTitle(), ipAddress);

        if (notificationService != null && enrollmentRepository != null) {
            List<com.virtualclassroom.model.StudentSubjectEnrollment> enrollments = enrollmentRepository.findBySubjectId(subject.getId());
            for (com.virtualclassroom.model.StudentSubjectEnrollment enrollment : enrollments) {
                notificationService.createNotification(
                        enrollment.getStudent(),
                        "Live Classroom Session Scheduled",
                        "Live session for " + subject.getTitle() + " has been scheduled for " + saved.getScheduledStart(),
                        "SESSION_SCHEDULED"
                );
            }
        }

        return LiveSessionDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<LiveSessionDto> getSessionsForSubject(Integer subjectId, User currentUser) {
        verifyAccessToSubject(currentUser, subjectId);
        return liveSessionRepository.findBySubjectIdOrderByScheduledStartDesc(subjectId).stream()
                .map(LiveSessionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<LiveSessionDto> getSessionsForSubjectPaged(Integer subjectId, User currentUser, Pageable pageable) {
        verifyAccessToSubject(currentUser, subjectId);
        return liveSessionRepository.findBySubjectId(subjectId, pageable)
                .map(LiveSessionDto::fromEntity);
    }

    public LiveSessionDto updateSessionStatus(Long sessionId, LiveSessionStatus newStatus, User facultyUser, String ipAddress) {
        LiveSession session = liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Live session not found with ID: " + sessionId));

        if (facultyUser.getRole() != Role.ROLE_ADMIN && !session.getHost().getId().equals(facultyUser.getId())) {
            throw new AccessDeniedException("Access denied: Only the host or an administrator can update session status");
        }

        LiveSessionStatus currentStatus = session.getStatus();

        // Terminal state guards
        if (currentStatus == LiveSessionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot change status of a session that is already COMPLETED");
        }
        if (currentStatus == LiveSessionStatus.CANCELLED) {
            throw new IllegalStateException("Cannot change status of a session that has been CANCELLED");
        }

        // State machine transition validation
        if (currentStatus == LiveSessionStatus.SCHEDULED) {
            if (newStatus != LiveSessionStatus.LIVE && newStatus != LiveSessionStatus.CANCELLED) {
                throw new IllegalStateException("Invalid status transition from SCHEDULED to " + newStatus + ". Permissible: LIVE, CANCELLED");
            }
        } else if (currentStatus == LiveSessionStatus.LIVE) {
            if (newStatus != LiveSessionStatus.COMPLETED && newStatus != LiveSessionStatus.CANCELLED) {
                throw new IllegalStateException("Invalid status transition from LIVE to " + newStatus + ". Permissible: COMPLETED, CANCELLED");
            }
        }

        session.setStatus(newStatus);
        LiveSession updated = liveSessionRepository.save(session);

        auditLogService.logAction(facultyUser, "UPDATE_SESSION_STATUS", "LiveSession", updated.getId().toString(),
                "Status: " + newStatus, ipAddress);

        return LiveSessionDto.fromEntity(updated);
    }

    private void verifyAccessToSubject(User user, Integer subjectId) {
        if (user.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        if (user.getRole() == Role.ROLE_FACULTY) {
            if (!facultyAssignmentRepository.existsByFacultyIdAndSubjectId(user.getId(), subjectId)) {
                throw new AccessDeniedException("Access denied: You are not assigned to this subject");
            }
        } else if (user.getRole() == Role.ROLE_STUDENT) {
            if (!enrollmentRepository.existsByStudentIdAndSubjectId(user.getId(), subjectId)) {
                throw new AccessDeniedException("Access denied: You are not enrolled in this subject");
            }
        }
    }
}

