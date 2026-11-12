package com.virtualclassroom.repository;

import com.virtualclassroom.model.StudentSubjectEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentSubjectEnrollmentRepository extends JpaRepository<StudentSubjectEnrollment, Long> {
    List<StudentSubjectEnrollment> findByStudentId(Long studentId);
    List<StudentSubjectEnrollment> findBySubjectId(Integer subjectId);
    Optional<StudentSubjectEnrollment> findByStudentIdAndSubjectId(Long studentId, Integer subjectId);
    boolean existsByStudentIdAndSubjectId(Long studentId, Integer subjectId);
    boolean existsByStudentIdAndSubjectIdAndAcademicTerm(Long studentId, Integer subjectId, String academicTerm);
}

