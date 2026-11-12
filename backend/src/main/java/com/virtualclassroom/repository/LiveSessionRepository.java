package com.virtualclassroom.repository;

import com.virtualclassroom.model.LiveSession;
import com.virtualclassroom.model.LiveSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LiveSessionRepository extends JpaRepository<LiveSession, Long> {
    List<LiveSession> findBySubjectIdOrderByScheduledStartDesc(Integer subjectId);
    Page<LiveSession> findBySubjectId(Integer subjectId, Pageable pageable);
    List<LiveSession> findByHostIdOrderByScheduledStartDesc(Long hostId);
    List<LiveSession> findBySubjectIdInAndStatusOrderByScheduledStartAsc(Collection<Integer> subjectIds, LiveSessionStatus status);
}
