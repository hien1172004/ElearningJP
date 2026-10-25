package com.jp.elearningjp.repository.exam;

import com.jp.elearningjp.entity.exam.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

    List<StudentAnswer> findByAttemptIdAndDeletedFalse(Long attemptId);

    Optional<StudentAnswer> findByAttemptIdAndQuestionIdAndDeletedFalse(Long attemptId, Long questionId);

    @Query("SELECT sa FROM StudentAnswer sa " +
           "JOIN FETCH sa.question q " +
           "LEFT JOIN FETCH sa.selectedOption " +
           "LEFT JOIN FETCH q.options " +
           "LEFT JOIN FETCH q.passage " +
           "WHERE sa.attempt.id = :attemptId AND sa.deleted = false")
    List<StudentAnswer> findByAttemptIdWithDetails(@Param("attemptId") Long attemptId);
}
