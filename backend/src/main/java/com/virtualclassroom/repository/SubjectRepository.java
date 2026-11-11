package com.virtualclassroom.repository;

import com.virtualclassroom.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    Optional<Subject> findByCode(String code);
    List<Subject> findByCourseId(Integer courseId);
    List<Subject> findByCourseIdAndYearLevel(Integer courseId, Integer yearLevel);
}
