package com.virtualclassroom.repository;

import com.virtualclassroom.model.FacultyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyProfileRepository extends JpaRepository<FacultyProfile, Long> {
    Optional<FacultyProfile> findByEmployeeId(String employeeId);
    boolean existsByEmployeeId(String employeeId);
}

