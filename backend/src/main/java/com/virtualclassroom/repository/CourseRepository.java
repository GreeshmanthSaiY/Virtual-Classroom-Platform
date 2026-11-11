package com.virtualclassroom.repository;

import com.virtualclassroom.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {
    Optional<Course> findByCode(String code);
    List<Course> findByDegreeId(Integer degreeId);
}

