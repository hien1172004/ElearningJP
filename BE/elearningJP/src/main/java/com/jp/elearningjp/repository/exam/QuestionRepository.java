package com.jp.elearningjp.repository.exam;

import com.jp.elearningjp.entity.exam.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    Optional<Question> findByIdAndDeletedFalse(Long id);

    @Query("SELECT q FROM Question q " +
           "LEFT JOIN FETCH q.options " +
           "LEFT JOIN FETCH q.passage " +
           "LEFT JOIN FETCH q.mondaiType " +
           "WHERE q.id = :id AND q.deleted = false")
    Optional<Question> findByIdWithOptionsAndPassage(@Param("id") Long id);
}
