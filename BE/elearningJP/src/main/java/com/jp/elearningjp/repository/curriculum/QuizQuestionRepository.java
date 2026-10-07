package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(Long quizId);

    Optional<QuizQuestion> findByIdAndDeletedFalse(Long id);

    long countByQuizIdAndDeletedFalse(Long quizId);

    @Query("SELECT COALESCE(MAX(qq.orderIndex), 0) FROM QuizQuestion qq WHERE qq.quiz.id = :quizId AND qq.deleted = false")
    Integer findMaxOrderIndexByQuizId(@Param("quizId") Long quizId);
}
