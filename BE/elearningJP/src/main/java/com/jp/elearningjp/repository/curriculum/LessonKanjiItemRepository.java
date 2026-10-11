package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonKanjiItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonKanjiItemRepository extends JpaRepository<LessonKanjiItem, Long> {
    List<LessonKanjiItem> findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(Long sectionId);
    List<LessonKanjiItem> findBySectionLessonIdAndDeletedFalseOrderByOrderIndexAsc(Long lessonId);

    /**
     * Batch fetch tất cả KANJI items của 1 lesson, kèm character (many-to-1 — OK to fetch).
     * KHÔNG fetch relatedVocabulary ở đây (cũng là List) — dùng
     * {@link LessonKanjiRelatedVocabRepository#findBySectionIds} riêng.
     */
    @Query("SELECT DISTINCT ki FROM LessonKanjiItem ki LEFT JOIN FETCH ki.character WHERE ki.section.lesson.id = :lessonId AND ki.deleted = false ORDER BY ki.orderIndex ASC")
    List<LessonKanjiItem> findByLessonIdWithCharacter(@Param("lessonId") Long lessonId);
}
