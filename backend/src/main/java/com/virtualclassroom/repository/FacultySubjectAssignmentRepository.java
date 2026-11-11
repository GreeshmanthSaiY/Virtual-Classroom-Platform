package com.virtualclassroom.repository;

import com.virtualclassroom.model.FacultySubjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacultySubjectAssignmentRepository extends JpaRepository<FacultySubjectAssignment, FacultySubjectAssignment.AssignmentId> {
    List<FacultySubjectAssignment> findByFacultyId(Long facultyId);
    List<FacultySubjectAssignment> findBySubjectId(Integer subjectId);
}

