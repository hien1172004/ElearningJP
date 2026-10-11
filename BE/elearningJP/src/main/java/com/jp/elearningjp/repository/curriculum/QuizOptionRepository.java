package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.QuizOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizOptionRepository extends JpaRepository<QuizOption, Long> {

    List<QuizOption> findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(Long questionId);

    Optional<QuizOption> findByQuestionIdAndCorrectTrueAndDeletedFalse(Long questionId);

    Optional<QuizOption> findByIdAndDeletedFalse(Long id);

    /** Batch fetch options cho tất cả questions của 1 lesson (qua quiz). */
    @Query("SELECT o FROM QuizOption o WHERE o.question.quiz.lesson.id = :lessonId AND o.deleted = false ORDER BY o.question.id, o.optionKey ASC")
    List<QuizOption> findByLessonId(@Param("lessonId") Long lessonId);
}
