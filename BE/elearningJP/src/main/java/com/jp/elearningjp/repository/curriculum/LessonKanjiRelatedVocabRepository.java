package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonKanjiRelatedVocab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LessonKanjiRelatedVocabRepository extends JpaRepository<LessonKanjiRelatedVocab, Long> {
    List<LessonKanjiRelatedVocab> findByKanjiItemIdAndDeletedFalse(Long kanjiItemId);

    /** Lấy tất cả related vocab (kể cả soft-delete) của 1 kanji item — dùng cho logic diff/replace. */
    List<LessonKanjiRelatedVocab> findByKanjiItemId(Long kanjiItemId);

    /** Batch fetch related vocab cho tất cả kanji items trong các section của 1 lesson. */
    @Query("SELECT rv FROM LessonKanjiRelatedVocab rv WHERE rv.kanjiItem.section.id IN :sectionIds AND rv.deleted = false ORDER BY rv.kanjiItem.id")
    List<LessonKanjiRelatedVocab> findBySectionIds(@Param("sectionIds") List<Long> sectionIds);

    /**
     * Batch fetch teacher-selected related vocab theo danh sách {@code kanjiItemIds}.
     * Trả về kèm eager fetch vocab để tránh N+1 khi build response.
     */
    @Query("SELECT DISTINCT rv FROM LessonKanjiRelatedVocab rv " +
           "LEFT JOIN FETCH rv.vocab v " +
           "WHERE rv.kanjiItem.id IN :kanjiItemIds " +
           "AND rv.deleted = false " +
           "ORDER BY rv.kanjiItem.id, rv.id ASC")
    List<LessonKanjiRelatedVocab> findByKanjiItemIdsWithVocab(@Param("kanjiItemIds") Collection<Long> kanjiItemIds);
}