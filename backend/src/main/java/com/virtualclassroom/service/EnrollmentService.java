package com.virtualclassroom.service;

import com.virtualclassroom.dto.StudentEnrollmentDto;
import com.virtualclassroom.model.EnrollmentStatus;
import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.StudentProfile;
import com.virtualclassroom.model.StudentSubjectEnrollment;
import com.virtualclassroom.model.Subject;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.StudentProfileRepository;
import com.virtualclassroom.repository.StudentSubjectEnrollmentRepository;
import com.virtualclassroom.repository.SubjectRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EnrollmentService {

    private final StudentSubjectEnrollmentRepository enrollmentRepository;
    private final StudentProfileRepository profileRepository;
    private final SubjectRepository subjectRepository;
    private final AuditLogService auditLogService;

    public EnrollmentService(
            StudentSubjectEnrollmentRepository enrollmentRepository,
            StudentProfileRepository profileRepository,
            SubjectRepository subjectRepository,
            AuditLogService auditLogService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.profileRepository = profileRepository;
        this.subjectRepository = subjectRepository;
        this.auditLogService = auditLogService;
    }

    public StudentEnrollmentDto enrollStudentInSubject(Long studentId, Integer subjectId, String term, User requester, String ipAddress) {
        if (requester.getRole() != Role.ROLE_ADMIN && !requester.getId().equals(studentId)) {
            throw new AccessDeniedException("Access denied: You cannot enroll another student");
        }

        StudentProfile profile = profileRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found for user ID: " + studentId));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found with ID: " + subjectId));

        // Academic integrity check: Subject must belong to student's course and year level
        if (!subject.getCourse().getId().equals(profile.getCourse().getId())) {
            throw new IllegalArgumentException("Cannot enroll: Subject belongs to a different degree program (" +
                    subject.getCourse().getCode() + " vs " + profile.getCourse().getCode() + ")");
        }
        if (!subject.getYearLevel().equals(profile.getCurrentYear())) {
            throw new IllegalArgumentException("Cannot enroll: Subject year level (" + subject.getYearLevel() +
                    ") does not match student's current year (" + profile.getCurrentYear() + ")");
        }

        String academicTerm = (term != null && !term.isBlank()) ? term : "2026-ACTIVE";

        if (enrollmentRepository.existsByStudentIdAndSubjectIdAndAcademicTerm(studentId, subjectId, academicTerm)) {
            throw new IllegalArgumentException("Student is already enrolled in this subject for term: " + academicTerm);
        }

        StudentSubjectEnrollment enrollment = StudentSubjectEnrollment.builder()
                .student(profile.getUser())
                .subject(subject)
                .academicTerm(academicTerm)
                .status(EnrollmentStatus.ENROLLED)
                .build();

        StudentSubjectEnrollment saved = enrollmentRepository.save(enrollment);
        auditLogService.logAction(requester, "ENROLL_SUBJECT", "StudentSubjectEnrollment", saved.getId().toString(),
                "Subject: " + subject.getCode() + ", Term: " + academicTerm, ipAddress);

        return StudentEnrollmentDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<StudentEnrollmentDto> getStudentEnrollments(Long studentId, User requester) {
        if (requester.getRole() != Role.ROLE_ADMIN && !requester.getId().equals(studentId)) {
            throw new AccessDeniedException("Access denied: You can only view your own enrollments");
        }

        return enrollmentRepository.findByStudentId(studentId).stream()
                .map(StudentEnrollmentDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentEnrollmentDto> getSubjectEnrollments(Integer subjectId) {
        return enrollmentRepository.findBySubjectId(subjectId).stream()
                .map(StudentEnrollmentDto::fromEntity)
                .collect(Collectors.toList());
    }
}

