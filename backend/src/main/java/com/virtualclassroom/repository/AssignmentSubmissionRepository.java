package com.virtualclassroom.repository;

import com.virtualclassroom.model.AssignmentSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    List<AssignmentSubmission> findByAssignmentId(Long assignmentId);
    Page<AssignmentSubmission> findByAssignmentId(Long assignmentId, Pageable pageable);
    List<AssignmentSubmission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    @Query("SELECT s FROM AssignmentSubmission s WHERE s.assignment.faculty.id = :facultyId AND s.gradePoints IS NULL ORDER BY s.submittedAt ASC")
    List<AssignmentSubmission> findPendingGradingByFacultyId(@Param("facultyId") Long facultyId);

    @Query("SELECT COUNT(s) FROM AssignmentSubmission s WHERE s.assignment.faculty.id = :facultyId AND s.gradePoints IS NULL")
    long countPendingGradingByFacultyId(@Param("facultyId") Long facultyId);
}

