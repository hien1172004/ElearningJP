package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * {@link com.jp.elearningjp.service.curriculum.SectionService#loadNestedCollections}
     * qua các method batch trong {@link LessonKanjiItemRepository},
     * {@link LessonVocabularyItemRepository}, {@link LessonGrammarItemRepository},
     * {@link com.jp.elearningjp.repository.curriculum.QuizRepository}.
     * Cùng persistence context → Hibernate tự gắn dữ liệu vào các section đã load.</p>
     */
    @Query("SELECT DISTINCT s FROM LessonSection s WHERE s.lesson.id = :lessonId AND s.deleted = false ORDER BY s.orderIndex ASC")
    List<LessonSection> findByLessonIdWithAllItems(@Param("lessonId") Long lessonId);
}
