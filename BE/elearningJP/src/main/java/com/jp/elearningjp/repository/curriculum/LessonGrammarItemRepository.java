package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonGrammarItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonGrammarItemRepository extends JpaRepository<LessonGrammarItem, Long> {
    List<LessonGrammarItem> findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(Long sectionId);

    /**
     * Fetch grammar item kèm examples (1-N lazy) — dùng trong update/edit để tránh LazyInitializationException.
     */
    @Query("SELECT gi FROM LessonGrammarItem gi LEFT JOIN FETCH gi.examples WHERE gi.id = :id AND gi.deleted = false")
    java.util.Optional<LessonGrammarItem> findByIdWithExamples(@Param("id") Long id);

    @Query("""
            SELECT gi.id
              FROM LessonGrammarItem gi
             WHERE gi.section.id = :sectionId
               AND gi.orderIndex = :orderIndex
               AND gi.deleted = false
            """)
    Long findIdBySectionIdAndOrderIndex(
            @Param("sectionId") Long sectionId,
            @Param("orderIndex") Integer orderIndex);
    List<LessonGrammarItem> findBySectionLessonIdAndDeletedFalseOrderByOrderIndexAsc(Long lessonId);

    /** Batch fetch GRAMMAR items kèm grammar (many-to-1) — KHÔNG fetch examples (Bag). */
    @Query("SELECT DISTINCT gi FROM LessonGrammarItem gi LEFT JOIN FETCH gi.grammar WHERE gi.section.lesson.id = :lessonId AND gi.deleted = false ORDER BY gi.orderIndex ASC")
    List<LessonGrammarItem> findByLessonIdWithGrammar(@Param("lessonId") Long lessonId);

    // ========================================================================
    // ORDER INDEX HELPERS — bulk update SQL để tránh UNIQUE conflict khi move item
    // ========================================================================

    @Modifying
    @Query("""
            UPDATE LessonGrammarItem gi
               SET gi.orderIndex = gi.orderIndex + 1
             WHERE gi.section.id = :sectionId
               AND gi.orderIndex >= :fromIndex
               AND gi.orderIndex <= :toIndex
               AND gi.deleted = false
               AND (:excludeId IS NULL OR gi.id <> :excludeId)
            """)
    int shiftOrderIndexesUpExcluding(
            @Param("sectionId") Long sectionId,
            @Param("fromIndex") Integer fromIndex,
            @Param("toIndex") Integer toIndex,
            @Param("excludeId") Long excludeId);

    @Modifying
    @Query("""
            UPDATE LessonGrammarItem gi
               SET gi.orderIndex = gi.orderIndex - 1
             WHERE gi.section.id = :sectionId
               AND gi.orderIndex > :fromIndex
               AND gi.orderIndex <= :toIndex
               AND gi.deleted = false
               AND (:excludeId IS NULL OR gi.id <> :excludeId)
            """)
    int shiftOrderIndexesDownInRangeExcluding(
            @Param("sectionId") Long sectionId,
            @Param("fromIndex") Integer fromIndex,
            @Param("toIndex") Integer toIndex,
            @Param("excludeId") Long excludeId);

    @Modifying
    @Query(value = """
            UPDATE lesson_grammar_items
               SET order_index = -ABS(order_index)
             WHERE id = :itemId
            """, nativeQuery = true)
    int setOrderIndexToNegativeMarker(@Param("itemId") Long itemId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE lesson_grammar_items
               SET order_index = CASE id
                   WHEN :idA THEN -99999
                   WHEN :idB THEN -99998
                   ELSE order_index
               END
             WHERE id IN (:idA, :idB)
               AND deleted = false
            """, nativeQuery = true)
    int setSwapMarkers(
            @Param("idA") Long idA,
            @Param("idB") Long idB);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE lesson_grammar_items
               SET order_index = CASE id
                   WHEN :idA THEN :orderIndexA
                   WHEN :idB THEN :orderIndexB
                   ELSE order_index
               END
             WHERE id IN (:idA, :idB)
               AND deleted = false
            """, nativeQuery = true)
    int setNewOrderIndexesAfterMarker(
            @Param("idA") Long idA,
            @Param("orderIndexA") Integer orderIndexA,
            @Param("idB") Long idB,
            @Param("orderIndexB") Integer orderIndexB);

    /**
     * Convenience: swap 2-STEP với marker tạm âm, an toàn với UNIQUE constraint.
     * Xem comment chi tiết trong {@link LessonKanjiItemRepository#swapOrderIndexes}.
     */
    default int swapOrderIndexes(
            Long idA, Integer orderIndexA,
            Long idB, Integer orderIndexB) {
        setSwapMarkers(idA, idB);
        return setNewOrderIndexesAfterMarker(idA, orderIndexA, idB, orderIndexB);
    }
}
