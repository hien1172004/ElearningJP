package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonGrammarItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonGrammarItemRepository extends JpaRepository<LessonGrammarItem, Long> {

    List<LessonGrammarItem> findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(Long sectionId);

    List<LessonGrammarItem> findBySectionLessonIdAndDeletedFalseOrderByOrderIndexAsc(Long lessonId);

    /** Batch fetch GRAMMAR items kèm grammar (many-to-1) — KHÔNG fetch examples (Bag). */
    @Query("SELECT DISTINCT gi FROM LessonGrammarItem gi LEFT JOIN FETCH gi.grammar WHERE gi.section.lesson.id = :lessonId AND gi.deleted = false ORDER BY gi.orderIndex ASC")
    List<LessonGrammarItem> findByLessonIdWithGrammar(@Param("lessonId") Long lessonId);
}
