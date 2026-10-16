package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonVocabularyExample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonVocabularyExampleRepository extends JpaRepository<LessonVocabularyExample, Long> {
    List<LessonVocabularyExample> findByVocabItemIdAndDeletedFalseOrderByOrderIndexAsc(Long vocabItemId);

    /** Batch fetch examples cho tất cả vocab items trong các section của 1 lesson. */
    @Query("SELECT e FROM LessonVocabularyExample e WHERE e.vocabItem.section.id IN :sectionIds AND e.deleted = false ORDER BY e.vocabItem.id, e.orderIndex ASC")
    List<LessonVocabularyExample> findBySectionIds(@Param("sectionIds") List<Long> sectionIds);
}
