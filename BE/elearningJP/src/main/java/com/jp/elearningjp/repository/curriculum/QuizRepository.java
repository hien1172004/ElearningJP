package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho Quiz — chỉ dùng lesson_id (refactor V20).
 */
@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /**
     * Tìm quiz theo lessonId.
     * @return Quiz nếu có, không có thì empty.
     */
    Optional<Quiz> findByLessonIdAndDeletedFalse(Long lessonId);

    /**
     * Kiểm tra lesson đã có quiz chưa.
     */
    boolean existsByLessonIdAndDeletedFalse(Long lessonId);

    /**
     * Lấy tất cả quiz của 1 lesson kèm questions.
     * KHÔNG fetch options ở đây — dùng
     * {@link QuizOptionRepository#findByLessonId} để batch load options riêng.
     */
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.lesson.id = :lessonId AND q.deleted = false")
    List<Quiz> findByLessonIdWithQuestions(@Param("lessonId") Long lessonId);
}
