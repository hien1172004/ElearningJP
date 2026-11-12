package com.jp.elearningjp.repository.curriculum;

import java.util.Collection;
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
    Optional<Quiz> findByDraftIdAndDeletedFalse(Long draftId);

    /**
     * Kiểm tra lesson đã có quiz chưa.
     */
    boolean existsByDraftIdAndDeletedFalse(Long draftId);

    /** Trong các lesson cho trước, lấy id những lesson có quiz trong nháp đang dùng của phía {@code kind}. */
    @Query("SELECT q.lesson.id FROM Quiz q WHERE q.lesson.id IN :lessonIds AND q.deleted = false "
            + "AND q.draft.kind = :kind AND q.draft.status <> com.jp.elearningjp.shared.enums.DraftStatus.ARCHIVED")
    List<Long> findLessonIdsWithQuiz(@Param("lessonIds") Collection<Long> lessonIds,
                                     @Param("kind") com.jp.elearningjp.shared.enums.DraftKind kind);

    /**
     * Lấy tất cả quiz của 1 lesson kèm questions.
     * KHÔNG fetch options ở đây — dùng
     * {@link QuizOptionRepository#findByLessonId} để batch load options riêng.
     */
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.draft.id = :draftId AND q.deleted = false")
    List<Quiz> findByDraftIdWithQuestions(@Param("draftId") Long draftId);

    /**
     * Dòng mang id ổn định {@code originId} (V65) — id trong bản live có thể là id của dòng đã bị xóa khi nháp được
     * dựng lại; dùng để tìm bài chứa phần tử khi đọc theo id của bản live.
     */
    java.util.Optional<Quiz> findFirstByOriginIdOrderByIdDesc(Long originId);
}
