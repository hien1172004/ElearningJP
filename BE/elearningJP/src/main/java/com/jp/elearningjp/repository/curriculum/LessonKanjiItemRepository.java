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
     * {@link LessonKanjiRelatedVocabRepository#findByKanjiItemIdsWithVocab} riêng.
     */
    @Query("SELECT DISTINCT ki FROM LessonKanjiItem ki LEFT JOIN FETCH ki.character WHERE ki.section.draft.id = :draftId AND ki.deleted = false ORDER BY ki.orderIndex ASC")
    List<LessonKanjiItem> findByDraftIdWithCharacter(@Param("draftId") Long draftId);

    /**
     * Dòng mang id ổn định {@code originId} (V65) — id trong bản live có thể là id của dòng đã bị xóa khi nháp được
     * dựng lại; dùng để tìm bài chứa phần tử khi đọc theo id của bản live.
     */
    java.util.Optional<LessonKanjiItem> findFirstByOriginIdOrderByIdDesc(Long originId);
}
