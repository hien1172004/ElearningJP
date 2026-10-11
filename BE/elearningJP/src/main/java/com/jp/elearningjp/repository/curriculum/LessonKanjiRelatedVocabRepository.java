package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonKanjiRelatedVocab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonKanjiRelatedVocabRepository extends JpaRepository<LessonKanjiRelatedVocab, Long> {
    List<LessonKanjiRelatedVocab> findByKanjiItemIdAndDeletedFalse(Long kanjiItemId);

    /** Batch fetch related vocab cho tất cả kanji items trong các section của 1 lesson. */
    @Query("SELECT rv FROM LessonKanjiRelatedVocab rv WHERE rv.kanjiItem.section.id IN :sectionIds AND rv.deleted = false ORDER BY rv.kanjiItem.id")
    List<LessonKanjiRelatedVocab> findBySectionIds(@Param("sectionIds") List<Long> sectionIds);
}
