package com.virtualclassroom.service;

import com.virtualclassroom.dto.AssignmentDto;
import com.virtualclassroom.dto.AssignmentSubmissionDto;
import com.virtualclassroom.dto.CreateAssignmentRequest;
import com.virtualclassroom.dto.GradeSubmissionRequest;
import com.virtualclassroom.dto.SubmitAssignmentRequest;
import com.virtualclassroom.model.Assignment;
import com.virtualclassroom.model.AssignmentSubmission;
import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.SubmissionStatus;
import com.virtualclassroom.model.Subject;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.AssignmentRepository;
import com.virtualclassroom.repository.AssignmentSubmissionRepository;
import com.virtualclassroom.repository.FacultySubjectAssignmentRepository;
import com.virtualclassroom.repository.StudentSubjectEnrollmentRepository;
import com.virtualclassroom.repository.SubjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final SubjectRepository subjectRepository;
    private final FacultySubjectAssignmentRepository facultyAssignmentRepository;
    private final StudentSubjectEnrollmentRepository enrollmentRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            SubjectRepository subjectRepository,
            FacultySubjectAssignmentRepository facultyAssignmentRepository,
            StudentSubjectEnrollmentRepository enrollmentRepository,
            AuditLogService auditLogService
    ) {
        this(assignmentRepository, submissionRepository, subjectRepository, facultyAssignmentRepository,
                enrollmentRepository, auditLogService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AssignmentService(
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            SubjectRepository subjectRepository,
            FacultySubjectAssignmentRepository facultyAssignmentRepository,
            StudentSubjectEnrollmentRepository enrollmentRepository,
            AuditLogService auditLogService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) NotificationService notificationService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.subjectRepository = subjectRepository;
        this.facultyAssignmentRepository = facultyAssignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    public AssignmentDto createAssignment(CreateAssignmentRequest request, User facultyUser, String ipAddress) {
        if (facultyUser.getRole() != Role.ROLE_ADMIN) {
            boolean assigned = facultyAssignmentRepository.existsByFacultyIdAndSubjectId(facultyUser.getId(), request.getSubjectId());
            if (!assigned) {
                throw new AccessDeniedException("Access denied: You are not assigned to instruct this subject");
            }
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found with ID: " + request.getSubjectId()));

        Assignment assignment = Assignment.builder()
                .subject(subject)
                .faculty(facultyUser)
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .maxPoints(request.getMaxPoints())
                .attachmentUrl(request.getAttachmentUrl())
                .build();

        Assignment saved = assignmentRepository.save(assignment);
        auditLogService.logAction(facultyUser, "CREATE_ASSIGNMENT", "Assignment", saved.getId().toString(),
                "Title: " + saved.getTitle(), ipAddress);

        if (notificationService != null && enrollmentRepository != null) {
            List<com.virtualclassroom.model.StudentSubjectEnrollment> enrollments = enrollmentRepository.findBySubjectId(subject.getId());
            for (com.virtualclassroom.model.StudentSubjectEnrollment enrollment : enrollments) {
                notificationService.createNotification(
                        enrollment.getStudent(),
                        "New Assignment: " + saved.getTitle(),
                        "A new assignment has been posted for " + subject.getTitle() + ". Due: " + saved.getDueDate(),
                        "ASSIGNMENT_CREATED"
                );
            }
        }

        return AssignmentDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AssignmentDto> getAssignmentsForSubject(Integer subjectId, User currentUser) {
        verifyAccessToSubject(currentUser, subjectId);
        return assignmentRepository.findBySubjectIdOrderByDueDateAsc(subjectId).stream()
                .map(AssignmentDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AssignmentDto> getAssignmentsForSubjectPaged(Integer subjectId, User currentUser, Pageable pageable) {
        verifyAccessToSubject(currentUser, subjectId);
        return assignmentRepository.findBySubjectId(subjectId, pageable)
                .map(AssignmentDto::fromEntity);
    }

    public AssignmentSubmissionDto submitAssignment(SubmitAssignmentRequest request, User studentUser, String ipAddress) {
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found with ID: " + request.getAssignmentId()));

        // Verify student is enrolled in this subject
        boolean enrolled = enrollmentRepository.existsByStudentIdAndSubjectId(studentUser.getId(), assignment.getSubject().getId());
        if (!enrolled && studentUser.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Access denied: You are not enrolled in this academic subject");
        }

        SubmissionStatus status = Instant.now().isAfter(assignment.getDueDate())
                ? SubmissionStatus.LATE
                : SubmissionStatus.SUBMITTED;

        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), studentUser.getId())
                .orElse(AssignmentSubmission.builder()
                        .assignment(assignment)
                        .student(studentUser)
                        .build());

        submission.setSubmissionText(request.getSubmissionText());
        submission.setFileUrl(request.getFileUrl());
        submission.setStatus(status);

        AssignmentSubmission saved = submissionRepository.save(submission);
        auditLogService.logAction(studentUser, "SUBMIT_ASSIGNMENT", "AssignmentSubmission", saved.getId().toString(),
                "Assignment: " + assignment.getTitle(), ipAddress);

        return AssignmentSubmissionDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDto> getSubmissionsForAssignment(Long assignmentId, User facultyUser) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found with ID: " + assignmentId));

        if (facultyUser.getRole() != Role.ROLE_ADMIN) {
            boolean assigned = facultyAssignmentRepository.existsByFacultyIdAndSubjectId(facultyUser.getId(), assignment.getSubject().getId());
            if (!assigned) {
                throw new AccessDeniedException("Access denied: You are not assigned to instruct this subject");
            }
        }

        return submissionRepository.findByAssignmentId(assignmentId).stream()
                .map(AssignmentSubmissionDto::fromEntity)
                .collect(Collectors.toList());
    }

    public AssignmentSubmissionDto gradeSubmission(Long submissionId, GradeSubmissionRequest request, User facultyUser, String ipAddress) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));

        if (facultyUser.getRole() != Role.ROLE_ADMIN) {
            boolean assigned = facultyAssignmentRepository.existsByFacultyIdAndSubjectId(
                    facultyUser.getId(), submission.getAssignment().getSubject().getId());
            if (!assigned) {
                throw new AccessDeniedException("Access denied: You are not assigned to instruct this subject");
            }
        }

        submission.setGradePoints(request.getGradePoints());
        submission.setFeedback(request.getFeedback());
        submission.setGradedBy(facultyUser);
        submission.setGradedAt(Instant.now());
        submission.setStatus(SubmissionStatus.GRADED);

        AssignmentSubmission saved = submissionRepository.save(submission);
        auditLogService.logAction(facultyUser, "GRADE_ASSIGNMENT", "AssignmentSubmission", saved.getId().toString(),
                "Points: " + request.getGradePoints(), ipAddress);

        if (notificationService != null && saved.getStudent() != null) {
            notificationService.createNotification(
                    saved.getStudent(),
                    "Assignment Graded: " + saved.getAssignment().getTitle(),
                    "Your submission has been evaluated. Score: " + saved.getGradePoints() + "/" + saved.getAssignment().getMaxPoints(),
                    "ASSIGNMENT_GRADED"
            );
        }

        return AssignmentSubmissionDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDto> getStudentSubmissions(User studentUser) {
        return submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentUser.getId()).stream()
                .map(AssignmentSubmissionDto::fromEntity)
                .collect(Collectors.toList());
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

