package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonKanjiItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonKanjiItemRepository extends JpaRepository<LessonKanjiItem, Long> {
    List<LessonKanjiItem> findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(Long sectionId);

    /**
     * Tìm ID của kanji item ACTIVE trong section tại orderIndex cụ thể.
     * Dùng cho thuật toán adjacent-swap để biết neighbor id.
     */
    @Query("""
            SELECT ki.id
              FROM LessonKanjiItem ki
             WHERE ki.section.id = :sectionId
               AND ki.orderIndex = :orderIndex
               AND ki.deleted = false
            """)
    Long findIdBySectionIdAndOrderIndex(
            @Param("sectionId") Long sectionId,
            @Param("orderIndex") Integer orderIndex);
    List<LessonKanjiItem> findBySectionLessonIdAndDeletedFalseOrderByOrderIndexAsc(Long lessonId);

    /**
     * Batch fetch tất cả KANJI items của 1 lesson, kèm character (many-to-1 — OK to fetch).
     * KHÔNG fetch relatedVocabulary ở đây (cũng là List) — dùng
     * {@link LessonKanjiRelatedVocabRepository#findBySectionIds} riêng.
     */
    @Query("SELECT DISTINCT ki FROM LessonKanjiItem ki LEFT JOIN FETCH ki.character WHERE ki.section.lesson.id = :lessonId AND ki.deleted = false ORDER BY ki.orderIndex ASC")
    List<LessonKanjiItem> findByLessonIdWithCharacter(@Param("lessonId") Long lessonId);

    // ========================================================================
    // ORDER INDEX HELPERS — bulk update SQL (bypass Hibernate session) để
    // tránh UNIQUE conflict khi move item (vì dirty checking + flush có thể
    // sort UPDATE theo thứ tự không mong muốn).
    // ========================================================================

    /**
     * Shift TẤT CẢ kanji item trong section có orderIndex ∈ [fromIndex, toIndex]
     * lên +1, loại trừ item có id = excludeId.
     *
     * <p>Use case: khi move item từ oldIndex lên target (target &lt; oldIndex),
     * cần dời các item trong [target, oldIndex-1] lên +1.</p>
     */
    @Modifying
    @Query("""
            UPDATE LessonKanjiItem ki
               SET ki.orderIndex = ki.orderIndex + 1
             WHERE ki.section.id = :sectionId
               AND ki.orderIndex >= :fromIndex
               AND ki.orderIndex <= :toIndex
               AND ki.deleted = false
               AND (:excludeId IS NULL OR ki.id <> :excludeId)
            """)
    int shiftOrderIndexesUpExcluding(
            @Param("sectionId") Long sectionId,
            @Param("fromIndex") Integer fromIndex,
            @Param("toIndex") Integer toIndex,
            @Param("excludeId") Long excludeId);

    /**
     * Shift TẤT CẢ kanji item trong section có orderIndex ∈ (fromIndex, toIndex]
     * xuống -1, loại trừ item có id = excludeId.
     *
     * <p>Use case: khi move item từ oldIndex xuống target (target &gt; oldIndex),
     * cần dời các item trong (oldIndex, target] xuống -1.</p>
     */
    @Modifying
    @Query("""
            UPDATE LessonKanjiItem ki
               SET ki.orderIndex = ki.orderIndex - 1
             WHERE ki.section.id = :sectionId
               AND ki.orderIndex > :fromIndex
               AND ki.orderIndex <= :toIndex
               AND ki.deleted = false
               AND (:excludeId IS NULL OR ki.id <> :excludeId)
            """)
    int shiftOrderIndexesDownInRangeExcluding(
            @Param("sectionId") Long sectionId,
            @Param("fromIndex") Integer fromIndex,
            @Param("toIndex") Integer toIndex,
            @Param("excludeId") Long excludeId);

    /**
     * Set orderIndex của item hiện tại về marker tạm (-|oldIndex|) bằng native SQL
     * để bypass JPA dirty checking. Bắt buộc gọi TRƯỚC khi shift để tránh
     * UNIQUE conflict với row cũ.
     *
     * <p>Ví dụ: items = [1,2,3,4,5], move @5 → @2 (MOVE_UP)</p>
     * <ul>
     *   <li>Nếu KHÔNG set marker: shift [2,4] lên +1 → DB = [1,3,4,5,5] ← TRÙNG @5 → 23505</li>
     *   <li>Nếu set marker trước: DB = [1,2,3,4,-5]. Shift [2,4] lên +1 → [1,3,4,5,-5] ✓</li>
     * </ul>
     */
    @Modifying
    @Query(value = """
            UPDATE lesson_kanji_items
               SET order_index = -ABS(order_index)
             WHERE id = :itemId
            """, nativeQuery = true)
    int setOrderIndexToNegativeMarker(@Param("itemId") Long itemId);

    /**
     * Swap orderIndex của 2 kanji item trong cùng section bằng cơ chế 2-STEP
     * với marker tạm âm (-temp) để tránh UNIQUE conflict.
     *
     * <p>Use case: thuật toán sắp xếp theo swap liền kề. Mỗi lần muốn dịch chuyển
     * item 1 vị trí, gọi swapOrderIndexes(itemA, itemB) để đổi chỗ 2 item.</p>
     *
     * <p>Ví dụ: A(1), B(2), C(3), D(4) — swap A↔D → A(4), B(2), C(3), D(1).
     * Cả A và D đều trong cùng section, nên nếu update thẳng với CASE WHEN sẽ
     * gây UNIQUE conflict vì giữa chừng cả 2 cùng mang orderIndex cũ/mới.
     * Bằng cách set cả 2 về marker âm (-99999, -99998) trước, rồi mới set
     * giá trị mới, ta luôn đảm bảo không có 2 row cùng orderIndex tại bất kỳ
     * thời điểm nào.</p>
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE lesson_kanji_items
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

    /**
     * Sau khi đã set marker tạm, set orderIndex mới cho cả 2 row.
     * Bước 2 của cơ chế 2-step swap (xem {@link #setSwapMarkers}).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE lesson_kanji_items
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
     * Convenience method: swap 2-STEP có marker tạm. An toàn với UNIQUE constraint.
     * Marker dùng giá trị âm rất nhỏ (-99999, -99998) để chắc chắn không
     * đụng bất kỳ orderIndex hợp lệ nào (orderIndex luôn dương).
     */
    default int swapOrderIndexes(
            Long idA, Integer orderIndexA,
            Long idB, Integer orderIndexB) {
        setSwapMarkers(idA, idB);
        return setNewOrderIndexesAfterMarker(idA, orderIndexA, idB, orderIndexB);
    }
}
