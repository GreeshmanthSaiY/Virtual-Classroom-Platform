package com.virtualclassroom.repository;

import com.virtualclassroom.model.VideoLecture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoLectureRepository extends JpaRepository<VideoLecture, Long> {
    List<VideoLecture> findBySubjectId(Integer subjectId);
    Page<VideoLecture> findBySubjectId(Integer subjectId, Pageable pageable);
    List<VideoLecture> findByFacultyId(Long facultyId);
    List<VideoLecture> findByFacultyIdAndSubjectId(Long facultyId, Integer subjectId);
    List<VideoLecture> findBySubjectIdIn(List<Integer> subjectIds);
    long countByFacultyId(Long facultyId);
}

