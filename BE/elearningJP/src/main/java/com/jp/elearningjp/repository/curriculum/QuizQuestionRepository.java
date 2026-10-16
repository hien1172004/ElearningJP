package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Trả về order_index nhỏ nhất còn trống trong khoảng [1, maxOrder+1] cho 1 quiz.
     * Dùng để fill gap khi client không gửi orderIndex.
     *
     * <p>Ví dụ: có order 1,2,4,5 (max=5, đã xoá 3):
     *   - "gap" = order 3 (ưu tiên điền vào chỗ trống)
     * Nếu không có gap (1,2,3): trả về maxOrder+1 = 4.</p>
     *
     * <p>Native query dùng generate_series(1..MAX+1) EXCEPT active set để
     * lấy idx nhỏ nhất không có trong tập order_index hiện tại.</p>
     */
    @Query(value = """
            WITH active AS (
                SELECT order_index FROM quiz_questions
                WHERE quiz_id = :quizId AND deleted = FALSE
            )
            SELECT MIN(g.idx) FROM (
                SELECT generate_series(1, (SELECT COALESCE(MAX(order_index), 0) + 1 FROM active)) AS idx
                EXCEPT
                SELECT order_index FROM active
            ) g
            """, nativeQuery = true)
    Integer findFirstAvailableOrderIndex(@Param("quizId") Long quizId);

    /**
     * Kiểm tra quiz có câu hỏi nào (chưa xoá) đang dùng orderIndex chưa.
     * Dùng trước khi insert để trả lỗi 409 QUESTION_ORDER_CONFLICT sớm
     * thay vì để DB ném DataIntegrityViolationException.
     */
    @Query("""
            SELECT COUNT(qq) > 0 FROM QuizQuestion qq
            WHERE qq.quiz.id = :quizId
              AND qq.orderIndex = :orderIndex
              AND qq.deleted = false
            """)
    boolean existsByQuizIdAndOrderIndexExcludingId(
            @Param("quizId") Long quizId,
            @Param("orderIndex") Integer orderIndex);

    /**
     * Dời tất cả câu hỏi có orderIndex >= fromIndex lên +1.
     * Dùng khi chèn câu hỏi mới vào giữa danh sách để bảo toàn thứ tự.
     *
     * <p>Chỉ ảnh hưởng các bản ghi CHƯA xoá (deleted = false).</p>
     *
     * @return số bản ghi bị ảnh hưởng
     */
    @Modifying
    @Query("""
            UPDATE QuizQuestion qq
               SET qq.orderIndex = qq.orderIndex + 1
             WHERE qq.quiz.id = :quizId
               AND qq.orderIndex >= :fromIndex
               AND qq.deleted = false
            """)
    int shiftOrderIndexesUp(
            @Param("quizId") Long quizId,
            @Param("fromIndex") Integer fromIndex);
}
