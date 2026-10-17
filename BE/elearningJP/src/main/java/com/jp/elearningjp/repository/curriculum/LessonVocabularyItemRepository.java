package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonVocabularyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonVocabularyItemRepository extends JpaRepository<LessonVocabularyItem, Long> {
    List<LessonVocabularyItem> findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(Long sectionId);

    @Query("""
            SELECT vi.id
              FROM LessonVocabularyItem vi
             WHERE vi.section.id = :sectionId
               AND vi.orderIndex = :orderIndex
               AND vi.deleted = false
            """)
    Long findIdBySectionIdAndOrderIndex(
            @Param("sectionId") Long sectionId,
            @Param("orderIndex") Integer orderIndex);
    List<LessonVocabularyItem> findBySectionLessonIdAndDeletedFalseOrderByOrderIndexAsc(Long lessonId);

    /** Batch fetch VOCAB items kèm vocab (many-to-1) — KHÔNG fetch examples (Bag). */
    @Query("SELECT DISTINCT vi FROM LessonVocabularyItem vi LEFT JOIN FETCH vi.vocab WHERE vi.section.lesson.id = :lessonId AND vi.deleted = false ORDER BY vi.orderIndex ASC")
    List<LessonVocabularyItem> findByLessonIdWithVocab(@Param("lessonId") Long lessonId);

    // ========================================================================
    // ORDER INDEX HELPERS — bulk update SQL để tránh UNIQUE conflict khi move item
    // ========================================================================

    @Modifying
    @Query("""
            UPDATE LessonVocabularyItem vi
               SET vi.orderIndex = vi.orderIndex + 1
             WHERE vi.section.id = :sectionId
               AND vi.orderIndex >= :fromIndex
               AND vi.orderIndex <= :toIndex
               AND vi.deleted = false
               AND (:excludeId IS NULL OR vi.id <> :excludeId)
            """)
    int shiftOrderIndexesUpExcluding(
            @Param("sectionId") Long sectionId,
            @Param("fromIndex") Integer fromIndex,
            @Param("toIndex") Integer toIndex,
            @Param("excludeId") Long excludeId);

    @Modifying
    @Query("""
            UPDATE LessonVocabularyItem vi
               SET vi.orderIndex = vi.orderIndex - 1
             WHERE vi.section.id = :sectionId
               AND vi.orderIndex > :fromIndex
               AND vi.orderIndex <= :toIndex
               AND vi.deleted = false
               AND (:excludeId IS NULL OR vi.id <> :excludeId)
            """)
    int shiftOrderIndexesDownInRangeExcluding(
            @Param("sectionId") Long sectionId,
            @Param("fromIndex") Integer fromIndex,
            @Param("toIndex") Integer toIndex,
            @Param("excludeId") Long excludeId);

    @Modifying
    @Query(value = """
            UPDATE lesson_vocabulary_items
               SET order_index = -ABS(order_index)
             WHERE id = :itemId
            """, nativeQuery = true)
    int setOrderIndexToNegativeMarker(@Param("itemId") Long itemId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE lesson_vocabulary_items
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
            UPDATE lesson_vocabulary_items
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
