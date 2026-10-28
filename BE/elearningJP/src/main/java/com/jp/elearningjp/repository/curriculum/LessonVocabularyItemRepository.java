package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonVocabularyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonVocabularyItemRepository extends JpaRepository<LessonVocabularyItem, Long> {

    List<LessonVocabularyItem> findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(Long sectionId);

    List<LessonVocabularyItem> findBySectionLessonIdAndDeletedFalseOrderByOrderIndexAsc(Long lessonId);

    /** Batch fetch VOCAB items kèm vocab (many-to-1) — KHÔNG fetch examples (Bag). */
    @Query("SELECT DISTINCT vi FROM LessonVocabularyItem vi LEFT JOIN FETCH vi.vocab WHERE vi.section.lesson.id = :lessonId AND vi.deleted = false ORDER BY vi.orderIndex ASC")
    List<LessonVocabularyItem> findByLessonIdWithVocab(@Param("lessonId") Long lessonId);
}
