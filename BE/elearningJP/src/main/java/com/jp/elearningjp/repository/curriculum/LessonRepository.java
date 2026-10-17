package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(Long courseId);

    Optional<Lesson> findByIdAndDeletedFalse(Long id);

    /**
     * Tìm ID của lesson ACTIVE trong course tại orderIndex cụ thể.
     */
    @Query("""
            SELECT l.id
              FROM Lesson l
             WHERE l.course.id = :courseId
               AND l.orderIndex = :orderIndex
               AND l.deleted = false
            """)
    Long findIdByCourseIdAndOrderIndex(
            @Param("courseId") Long courseId,
            @Param("orderIndex") Integer orderIndex);

    long countByCourseIdAndDeletedFalse(Long courseId);

    long countByCourse_IdAndDeletedFalse(Long courseId);

    @Query("SELECT COALESCE(MAX(l.orderIndex), 0) FROM Lesson l WHERE l.course.id = :courseId AND l.deleted = false")
    Integer findMaxOrderIndexByCourseId(@Param("courseId") Long courseId);

    /**
     * Lấy 1 lesson + tất cả sections (đã sắp xếp).
     * Tránh N+1 khi truy cập lesson.getSections().
     */
    @Query("""
            SELECT DISTINCT l FROM Lesson l
            LEFT JOIN FETCH l.sections s
            WHERE l.id = :id AND l.deleted = false
            ORDER BY s.orderIndex ASC
            """)
    Optional<Lesson> findByIdWithSections(@Param("id") Long id);

    // ========================================================================
    // ORDER INDEX HELPERS — dùng để tránh trùng order_index (V19+)
    // ========================================================================

    /**
     * Kiểm tra lesson active khác có cùng (courseId, orderIndex) hay không.
     * Dùng trước khi insert/update để trả lỗi 409 LESSON_ORDER_CONFLICT
     * thay vì để Postgres ném DataIntegrityViolationException.
     */
    @Query("""
            SELECT COUNT(l) > 0 FROM Lesson l
            WHERE l.course.id = :courseId
              AND l.orderIndex = :orderIndex
              AND l.deleted = false
              AND (:excludeId IS NULL OR l.id <> :excludeId)
            """)
    boolean existsByCourseIdAndOrderIndexExcludingId(
            @Param("courseId") Long courseId,
            @Param("orderIndex") Integer orderIndex,
            @Param("excludeId") Long excludeId);

    /**
     * Dời tất cả lesson có orderIndex >= fromIndex lên +1.
     * Dùng khi chèn lesson mới vào giữa danh sách để bảo toàn thứ tự.
     *
     * <p>Chỉ ảnh hưởng các bản ghi CHƯA xoá (deleted = false).</p>
     *
     * @return số bản ghi bị ảnh hưởng
     */
    @Modifying
    @Query("""
            UPDATE Lesson l
               SET l.orderIndex = l.orderIndex + 1
             WHERE l.course.id = :courseId
               AND l.orderIndex >= :fromIndex
               AND l.deleted = false
               AND (:excludeId IS NULL OR l.id <> :excludeId)
            """)
    int shiftOrderIndexesUp(
            @Param("courseId") Long courseId,
            @Param("fromIndex") Integer fromIndex,
            @Param("excludeId") Long excludeId);

    /**
     * Dời tất cả lesson có orderIndex trong (lowerExclusive, upperInclusive]
     * xuống -1. Dùng cho logic "move down" trong updateLesson.
     *
     * <p>Điều kiện excludeId đảm bảo lesson đang được cập nhật không bị
     * ảnh hưởng (vì nó đang ở marker tạm âm).</p>
     */
    @Modifying
    @Query("""
            UPDATE Lesson l
               SET l.orderIndex = l.orderIndex - 1
             WHERE l.course.id = :courseId
               AND l.orderIndex > :lowerExclusive
               AND l.orderIndex <= :upperInclusive
               AND l.deleted = false
               AND (:excludeId IS NULL OR l.id <> :excludeId)
            """)
    int shiftOrderIndexesDownInRange(
            @Param("courseId") Long courseId,
            @Param("lowerExclusive") Integer lowerExclusive,
            @Param("upperInclusive") Integer upperInclusive,
            @Param("excludeId") Long excludeId);

    /**
     * Set orderIndex của lesson hiện tại về marker tạm (-|oldIndex|) bằng native SQL.
     */
    @Modifying
    @Query(value = """
            UPDATE lessons
               SET order_index = -ABS(order_index)
             WHERE id = :lessonId
            """, nativeQuery = true)
    int setOrderIndexToNegativeMarker(@Param("lessonId") Long lessonId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE lessons
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
            UPDATE lessons
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
