package com.virtualclassroom.repository;

import com.virtualclassroom.model.Examination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ExaminationRepository extends JpaRepository<Examination, Long> {
    List<Examination> findBySubjectIdOrderByScheduledAtDesc(Integer subjectId);
    Page<Examination> findBySubjectId(Integer subjectId, Pageable pageable);
    List<Examination> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);
    List<Examination> findBySubjectIdInOrderByScheduledAtAsc(Collection<Integer> subjectIds);
}

