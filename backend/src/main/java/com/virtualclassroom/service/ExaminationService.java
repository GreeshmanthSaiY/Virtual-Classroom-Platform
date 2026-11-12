package com.virtualclassroom.service;

import com.virtualclassroom.dto.CreateExaminationRequest;
import com.virtualclassroom.dto.ExaminationDto;
import com.virtualclassroom.dto.ExaminationResultDto;
import com.virtualclassroom.dto.RecordGradeRequest;
import com.virtualclassroom.model.Examination;
import com.virtualclassroom.model.ExaminationResult;
import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.Subject;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.ExaminationRepository;
import com.virtualclassroom.repository.ExaminationResultRepository;
import com.virtualclassroom.repository.FacultySubjectAssignmentRepository;
import com.virtualclassroom.repository.StudentSubjectEnrollmentRepository;
import com.virtualclassroom.repository.SubjectRepository;
import com.virtualclassroom.repository.UserRepository;
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
public class ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final ExaminationResultRepository resultRepository;
    private final SubjectRepository subjectRepository;
    private final FacultySubjectAssignmentRepository facultyAssignmentRepository;
    private final StudentSubjectEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ExaminationService(
            ExaminationRepository examinationRepository,
            ExaminationResultRepository resultRepository,
            SubjectRepository subjectRepository,
            FacultySubjectAssignmentRepository facultyAssignmentRepository,
            StudentSubjectEnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            AuditLogService auditLogService
    ) {
        this.examinationRepository = examinationRepository;
        this.resultRepository = resultRepository;
        this.subjectRepository = subjectRepository;
        this.facultyAssignmentRepository = facultyAssignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public ExaminationDto createExamination(CreateExaminationRequest request, User creatorUser, String ipAddress) {
        if (creatorUser.getRole() != Role.ROLE_ADMIN) {
            boolean assigned = facultyAssignmentRepository.existsByFacultyIdAndSubjectId(creatorUser.getId(), request.getSubjectId());
            if (!assigned) {
                throw new AccessDeniedException("Access denied: You are not assigned to instruct this subject");
            }
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found with ID: " + request.getSubjectId()));

        Examination examination = Examination.builder()
                .subject(subject)
                .creator(creatorUser)
                .title(request.getTitle())
                .examType(request.getExamType())
                .totalMarks(request.getTotalMarks())
                .durationMinutes(request.getDurationMinutes())
                .scheduledAt(request.getScheduledAt())
                .instructions(request.getInstructions())
                .build();

        Examination saved = examinationRepository.save(examination);
        auditLogService.logAction(creatorUser, "CREATE_EXAMINATION", "Examination", saved.getId().toString(),
                "Title: " + saved.getTitle(), ipAddress);

        return ExaminationDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ExaminationDto> getExaminationsForSubject(Integer subjectId, User currentUser) {
        verifyAccessToSubject(currentUser, subjectId);
        return examinationRepository.findBySubjectIdOrderByScheduledAtDesc(subjectId).stream()
                .map(ExaminationDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ExaminationDto> getExaminationsForSubjectPaged(Integer subjectId, User currentUser, Pageable pageable) {
        verifyAccessToSubject(currentUser, subjectId);
        return examinationRepository.findBySubjectId(subjectId, pageable)
                .map(ExaminationDto::fromEntity);
    }

    public ExaminationResultDto recordGrade(RecordGradeRequest request, User facultyUser, String ipAddress) {
        Examination examination = examinationRepository.findById(request.getExaminationId())
                .orElseThrow(() -> new IllegalArgumentException("Examination not found with ID: " + request.getExaminationId()));

        if (facultyUser.getRole() != Role.ROLE_ADMIN) {
            boolean assigned = facultyAssignmentRepository.existsByFacultyIdAndSubjectId(
                    facultyUser.getId(), examination.getSubject().getId());
            if (!assigned) {
                throw new AccessDeniedException("Access denied: You are not assigned to instruct this subject");
            }
        }

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + request.getStudentId()));

        // Verify student is enrolled in this subject
        boolean enrolled = enrollmentRepository.existsByStudentIdAndSubjectId(student.getId(), examination.getSubject().getId());
        if (!enrolled) {
            throw new IllegalArgumentException("Cannot record grade: Student is not enrolled in this academic subject");
        }

        ExaminationResult result = resultRepository.findByExaminationIdAndStudentId(examination.getId(), student.getId())
                .orElse(ExaminationResult.builder()
                        .examination(examination)
                        .student(student)
                        .build());

        result.setMarksObtained(request.getMarksObtained());
        result.setGrade(request.getGrade());
        result.setRemarks(request.getRemarks());
        result.setGradedBy(facultyUser);
        result.setGradedAt(Instant.now());

        ExaminationResult saved = resultRepository.save(result);
        auditLogService.logAction(facultyUser, "RECORD_EXAM_GRADE", "ExaminationResult", saved.getId().toString(),
                "Marks: " + request.getMarksObtained() + ", Grade: " + request.getGrade(), ipAddress);

        return ExaminationResultDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ExaminationResultDto> getResultsForExamination(Long examinationId, User facultyUser) {
        Examination examination = examinationRepository.findById(examinationId)
                .orElseThrow(() -> new IllegalArgumentException("Examination not found with ID: " + examinationId));

        if (facultyUser.getRole() != Role.ROLE_ADMIN) {
            boolean assigned = facultyAssignmentRepository.existsByFacultyIdAndSubjectId(
                    facultyUser.getId(), examination.getSubject().getId());
            if (!assigned) {
                throw new AccessDeniedException("Access denied: You are not assigned to instruct this subject");
            }
        }

        return resultRepository.findByExaminationId(examinationId).stream()
                .map(ExaminationResultDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExaminationResultDto> getStudentResults(User studentUser) {
        return resultRepository.findByStudentIdOrderByGradedAtDesc(studentUser.getId()).stream()
                .map(ExaminationResultDto::fromEntity)
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
