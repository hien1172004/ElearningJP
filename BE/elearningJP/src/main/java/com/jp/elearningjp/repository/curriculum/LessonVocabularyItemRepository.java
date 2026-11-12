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

    List<LessonVocabularyItem> findBySectionDraftIdAndDeletedFalseOrderByOrderIndexAsc(Long draftId);

    /** Batch fetch VOCAB items kèm vocab (many-to-1) — KHÔNG fetch examples (Bag). */
    @Query("SELECT DISTINCT vi FROM LessonVocabularyItem vi LEFT JOIN FETCH vi.vocab WHERE vi.section.draft.id = :draftId AND vi.deleted = false ORDER BY vi.orderIndex ASC")
    List<LessonVocabularyItem> findByDraftIdWithVocab(@Param("draftId") Long draftId);

    /**
     * Dòng mang id ổn định {@code originId} (V65) — id trong bản live có thể là id của dòng đã bị xóa khi nháp được
     * dựng lại; dùng để tìm bài chứa phần tử khi đọc theo id của bản live.
     */
    java.util.Optional<LessonVocabularyItem> findFirstByOriginIdOrderByIdDesc(Long originId);
}
