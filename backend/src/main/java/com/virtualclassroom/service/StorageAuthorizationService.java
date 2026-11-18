package com.virtualclassroom.service;

import com.virtualclassroom.model.Assignment;
import com.virtualclassroom.model.AssignmentSubmission;
import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.StudyMaterial;
import com.virtualclassroom.repository.AssignmentRepository;
import com.virtualclassroom.repository.AssignmentSubmissionRepository;
import com.virtualclassroom.repository.FacultySubjectAssignmentRepository;
import com.virtualclassroom.repository.StudentSubjectEnrollmentRepository;
import com.virtualclassroom.repository.StudyMaterialRepository;
import com.virtualclassroom.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Enforces resource-level authorization before issuing signed download URLs
 * for objects stored in the private Supabase Storage bucket.
 *
 * All authorization checks fail closed (AccessDeniedException) if the caller
 * does not own or have enrolled/assigned access to the associated academic resource.
 */
@Service
public class StorageAuthorizationService {

    private final AssignmentSubmissionRepository submissionRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudentSubjectEnrollmentRepository enrollmentRepository;
    private final FacultySubjectAssignmentRepository facultyAssignmentRepository;

    public StorageAuthorizationService(
            AssignmentSubmissionRepository submissionRepository,
            StudyMaterialRepository studyMaterialRepository,
            AssignmentRepository assignmentRepository,
            StudentSubjectEnrollmentRepository enrollmentRepository,
            FacultySubjectAssignmentRepository facultyAssignmentRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.studyMaterialRepository = studyMaterialRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.facultyAssignmentRepository = facultyAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public void authorizeDownload(String storagePath, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication is required to access storage resources.");
        }
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("Storage path is required.");
        }

        String role = principal.getRole();
        Long userId = principal.getId();

        if (Role.ROLE_ADMIN.name().equals(role)) {
            return;
        }

        // 1. Check Assignment Submissions (highest sensitivity: student work & grades)
        List<AssignmentSubmission> submissions = submissionRepository.findBySubmissionStoragePath(storagePath);
        if (!submissions.isEmpty()) {
            boolean allowed = submissions.stream().anyMatch(sub -> isAuthorizedForSubmission(sub, userId, role));
            if (!allowed) {
                throw new AccessDeniedException("Access denied: You are not authorized to access this assignment submission.");
            }
            return;
        }

        // 2. Check Study Materials (subject-scoped academic resources)
        List<StudyMaterial> materials = studyMaterialRepository.findByStoragePath(storagePath);
        if (!materials.isEmpty()) {
            boolean allowed = materials.stream().anyMatch(mat -> isAuthorizedForMaterial(mat, userId, role));
            if (!allowed) {
                throw new AccessDeniedException("Access denied: You are not authorized to access this study material.");
            }
            return;
        }

        // 3. Check Assignment Attachments (subject-scoped coursework specifications)
        List<Assignment> assignments = assignmentRepository.findByAttachmentStoragePath(storagePath);
        if (!assignments.isEmpty()) {
            boolean allowed = assignments.stream().anyMatch(asg -> isAuthorizedForAssignment(asg, userId, role));
            if (!allowed) {
                throw new AccessDeniedException("Access denied: You are not authorized to access this assignment attachment.");
            }
            return;
        }

        // 4. Unlinked objects: profile photos are visible to authenticated classroom members
        if (storagePath.startsWith("profile-photos/")) {
            return;
        }

        // 5. Unlinked freshly uploaded file: allow only the original uploader (e.g. {folder}/{uploaderId}/{file})
        if (isOwnedByUserNamespace(storagePath, userId)) {
            return;
        }

        // 6. Fail closed for all unassociated or unauthorized paths
        throw new AccessDeniedException("Access denied: You are not authorized to access this storage resource.");
    }

    private boolean isAuthorizedForSubmission(AssignmentSubmission submission, Long userId, String role) {
        if (Role.ROLE_STUDENT.name().equals(role)) {
            return submission.getStudent() != null && userId.equals(submission.getStudent().getId());
        }
        if (Role.ROLE_FACULTY.name().equals(role)) {
            if (submission.getAssignment() == null) {
                return false;
            }
            if (submission.getAssignment().getFaculty() != null
                    && userId.equals(submission.getAssignment().getFaculty().getId())) {
                return true;
            }
            if (submission.getAssignment().getSubject() != null) {
                return facultyAssignmentRepository.existsByFacultyIdAndSubjectId(
                        userId, submission.getAssignment().getSubject().getId());
            }
        }
        return false;
    }

    private boolean isAuthorizedForMaterial(StudyMaterial material, Long userId, String role) {
        Integer subjectId = material.getSubject() != null ? material.getSubject().getId() : null;
        if (Role.ROLE_STUDENT.name().equals(role)) {
            return subjectId != null && enrollmentRepository.existsByStudentIdAndSubjectId(userId, subjectId);
        }
        if (Role.ROLE_FACULTY.name().equals(role)) {
            if (material.getUploader() != null && userId.equals(material.getUploader().getId())) {
                return true;
            }
            return subjectId != null && facultyAssignmentRepository.existsByFacultyIdAndSubjectId(userId, subjectId);
        }
        return false;
    }

    private boolean isAuthorizedForAssignment(Assignment assignment, Long userId, String role) {
        Integer subjectId = assignment.getSubject() != null ? assignment.getSubject().getId() : null;
        if (Role.ROLE_STUDENT.name().equals(role)) {
            return subjectId != null && enrollmentRepository.existsByStudentIdAndSubjectId(userId, subjectId);
        }
        if (Role.ROLE_FACULTY.name().equals(role)) {
            if (assignment.getFaculty() != null && userId.equals(assignment.getFaculty().getId())) {
                return true;
            }
            return subjectId != null && facultyAssignmentRepository.existsByFacultyIdAndSubjectId(userId, subjectId);
        }
        return false;
    }

    private boolean isOwnedByUserNamespace(String storagePath, Long userId) {
        String[] segments = storagePath.split("/");
        if (segments.length >= 3) {
            return String.valueOf(userId).equals(segments[1]);
        }
        return false;
    }
}
