package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonSectionRepository extends JpaRepository<LessonSection, Long> {

    /**
     * Lấy tất cả section (chưa xoá) của một lesson, sắp xếp theo orderIndex tăng dần.
     */
    List<LessonSection> findByLessonIdAndDeletedFalseOrderByOrderIndexAsc(Long lessonId);

    /**
     * Lấy section theo lesson + loại section (KANJI / VOCABULARY / GRAMMAR).
     */
    List<LessonSection> findByLessonIdAndSectionTypeAndDeletedFalseOrderByOrderIndexAsc(
            Long lessonId, LessonSectionType sectionType);

    /**
     * Kiểm tra lesson có chứa section thuộc loại nào đó hay không.
     * <p>SAU REFACTOR (V20/V21): KHÔNG dùng để xác định "lesson có quiz" nữa.
     * Hãy dùng {@link com.jp.elearningjp.repository.curriculum.QuizRepository#existsByLessonIdAndDeletedFalse}
     * (query thẳng bảng {@code lesson_quizzes}).</p>
     */
    boolean existsByLessonIdAndSectionTypeAndDeletedFalse(
            Long lessonId, LessonSectionType sectionType);

    /**
     * Chỉ load section cơ bản + lesson. KHÔNG JOIN FETCH bất kỳ collection nào
     * ở đây — Hibernate cấm fetch nhiều {@code List<>} cùng lúc (MultipleBagFetchException).
     * <p>Thay vào đó, các collection được load tách riêng trong
     * {@link com.jp.elearningjp.service.curriculum.LessonService#loadNestedCollections}
     * qua các method batch trong {@link LessonKanjiItemRepository},
     * {@link LessonVocabularyItemRepository}, {@link LessonGrammarItemRepository},
     * {@link com.jp.elearningjp.repository.curriculum.QuizRepository}.
     * Cùng persistence context → Hibernate tự gắn dữ liệu vào các section đã load.</p>
     */
    @Query("SELECT DISTINCT s FROM LessonSection s WHERE s.lesson.id = :lessonId AND s.deleted = false ORDER BY s.orderIndex ASC")
    List<LessonSection> findByLessonIdWithAllItems(@Param("lessonId") Long lessonId);

    // ========================================================================
    // ORDER INDEX HELPERS — dùng để tránh trùng order_index (V19+)
    // ========================================================================

    /**
     * Kiểm tra section khác (chưa xoá) trong cùng lesson có cùng orderIndex.
     * Dùng trước khi insert/update để trả lỗi 409 LESSON_ORDER_CONFLICT
     * thay vì để Postgres ném DataIntegrityViolationException.
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM LessonSection s
            WHERE s.lesson.id = :lessonId
              AND s.orderIndex = :orderIndex
              AND s.deleted = false
              AND (:excludeId IS NULL OR s.id <> :excludeId)
            """)
    boolean existsByLessonIdAndOrderIndexExcludingId(
            @Param("lessonId") Long lessonId,
            @Param("orderIndex") Integer orderIndex,
            @Param("excludeId") Long excludeId);

    /**
     * Dời tất cả section có orderIndex >= fromIndex lên +1 trong 1 lesson.
     * Chỉ ảnh hưởng bản ghi CHƯA xoá.
     */
    @Modifying
    @Query("""
            UPDATE LessonSection s
               SET s.orderIndex = s.orderIndex + 1
             WHERE s.lesson.id = :lessonId
               AND s.orderIndex >= :fromIndex
               AND s.deleted = false
               AND (:excludeId IS NULL OR s.id <> :excludeId)
            """)
    int shiftOrderIndexesUp(
            @Param("lessonId") Long lessonId,
            @Param("fromIndex") Integer fromIndex,
            @Param("excludeId") Long excludeId);

    /**
     * Dời tất cả section có orderIndex trong (lowerExclusive, upperInclusive]
     * xuống -1. Dùng cho logic "move down" trong updateSection.
     */
    @Modifying
    @Query("""
            UPDATE LessonSection s
               SET s.orderIndex = s.orderIndex - 1
             WHERE s.lesson.id = :lessonId
               AND s.orderIndex > :lowerExclusive
               AND s.orderIndex <= :upperInclusive
               AND s.deleted = false
               AND (:excludeId IS NULL OR s.id <> :excludeId)
            """)
    int shiftOrderIndexesDownInRange(
            @Param("lessonId") Long lessonId,
            @Param("lowerExclusive") Integer lowerExclusive,
            @Param("upperInclusive") Integer upperInclusive,
            @Param("excludeId") Long excludeId);
}
