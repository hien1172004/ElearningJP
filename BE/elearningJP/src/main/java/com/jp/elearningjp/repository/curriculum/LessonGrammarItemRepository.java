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

    List<LessonGrammarItem> findBySectionDraftIdAndDeletedFalseOrderByOrderIndexAsc(Long draftId);

    /** Batch fetch GRAMMAR items kèm grammar (many-to-1) — KHÔNG fetch examples (Bag). */
    @Query("SELECT DISTINCT gi FROM LessonGrammarItem gi LEFT JOIN FETCH gi.grammar WHERE gi.section.draft.id = :draftId AND gi.deleted = false ORDER BY gi.orderIndex ASC")
    List<LessonGrammarItem> findByDraftIdWithGrammar(@Param("draftId") Long draftId);

    /**
     * Dòng mang id ổn định {@code originId} (V65) — id trong bản live có thể là id của dòng đã bị xóa khi nháp được
     * dựng lại; dùng để tìm bài chứa phần tử khi đọc theo id của bản live.
     */
    java.util.Optional<LessonGrammarItem> findFirstByOriginIdOrderByIdDesc(Long originId);
}
