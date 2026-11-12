package com.virtualclassroom.repository;

import com.virtualclassroom.model.ExaminationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExaminationResultRepository extends JpaRepository<ExaminationResult, Long> {
    List<ExaminationResult> findByExaminationId(Long examinationId);
    Page<ExaminationResult> findByExaminationId(Long examinationId, Pageable pageable);
    List<ExaminationResult> findByStudentIdOrderByGradedAtDesc(Long studentId);
    Optional<ExaminationResult> findByExaminationIdAndStudentId(Long examinationId, Long studentId);
    boolean existsByExaminationIdAndStudentId(Long examinationId, Long studentId);
}
