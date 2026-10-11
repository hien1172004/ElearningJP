package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonGrammarExample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonGrammarExampleRepository extends JpaRepository<LessonGrammarExample, Long> {
    List<LessonGrammarExample> findByGrammarItemIdAndDeletedFalseOrderByOrderIndexAsc(Long grammarItemId);

    /** Batch fetch examples cho tất cả grammar items trong 1 lesson (tránh N+1). */
    @Query("SELECT e FROM LessonGrammarExample e WHERE e.grammarItem.section.id IN :sectionIds AND e.deleted = false ORDER BY e.grammarItem.id, e.orderIndex ASC")
    List<LessonGrammarExample> findBySectionIds(@Param("sectionIds") List<Long> sectionIds);
}
