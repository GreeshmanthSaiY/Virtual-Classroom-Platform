package com.virtualclassroom.repository;

import com.virtualclassroom.model.StudyMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    List<StudyMaterial> findBySubjectId(Integer subjectId);
    Page<StudyMaterial> findBySubjectId(Integer subjectId, Pageable pageable);
    List<StudyMaterial> findByUploaderId(Long uploaderId);
    List<StudyMaterial> findByUploaderIdAndSubjectId(Long uploaderId, Integer subjectId);
    List<StudyMaterial> findBySubjectIdIn(List<Integer> subjectIds);
}

