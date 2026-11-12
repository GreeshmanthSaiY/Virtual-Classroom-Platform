package com.virtualclassroom.repository;

import com.virtualclassroom.model.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findBySubjectIdOrderByDueDateAsc(Integer subjectId);
    Page<Assignment> findBySubjectId(Integer subjectId, Pageable pageable);
    List<Assignment> findByFacultyIdOrderByCreatedAtDesc(Long facultyId);
    List<Assignment> findBySubjectIdInOrderByDueDateAsc(Collection<Integer> subjectIds);
}
