package com.virtualclassroom.repository;

import com.virtualclassroom.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySubjectIdOrderByCreatedAtDesc(Integer subjectId);
    Page<Question> findBySubjectId(Integer subjectId, Pageable pageable);
    List<Question> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
    List<Question> findAllByOrderByCreatedAtDesc();
    long countByIsResolvedTrue();

    @Query("SELECT q FROM Question q WHERE " +
           "(:subjectId IS NULL OR q.subject.id = :subjectId) AND " +
           "(:query IS NULL OR :query = '' OR LOWER(q.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(q.body) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Question> searchQuestions(@Param("query") String query,
                                  @Param("subjectId") Integer subjectId,
                                  Pageable pageable);
}

