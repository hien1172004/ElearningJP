package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /**
     * Tìm quiz theo lessonId — CHỈ dùng cho data migration / tương thích ngược.
     * Logic nghiệp vụ mới: tìm quiz qua section TEST bằng
     * {@link #findFirstBySectionIdAndDeletedFalse(Long)}.
     */
    @Deprecated
    Optional<Quiz> findByLessonIdAndDeletedFalse(Long lessonId);

    boolean existsByLessonIdAndDeletedFalse(Long lessonId);

    /**
     * Tìm quiz duy nhất (nếu có) của 1 section TEST.
     * Mỗi section TEST chỉ chứa tối đa 1 quiz.
     */
    Optional<Quiz> findFirstBySectionIdAndDeletedFalse(Long sectionId);

    /**
     * Kiểm tra section đã có quiz chưa (dùng khi tạo mới để validate 1-1).
     */
    boolean existsBySectionIdAndDeletedFalse(Long sectionId);

    /**
     * Lấy tất cả quiz của 1 lesson kèm questions.
     * KHÔNG fetch options ở đây (cũng là Bag) — dùng
     * {@link QuizOptionRepository#findByLessonId} để batch load options riêng.
     */
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.lesson.id = :lessonId AND q.deleted = false")
    List<Quiz> findByLessonIdWithQuestions(@Param("lessonId") Long lessonId);
}
