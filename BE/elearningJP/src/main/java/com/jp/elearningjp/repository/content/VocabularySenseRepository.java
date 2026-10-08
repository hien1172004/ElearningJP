package com.jp.elearningjp.repository.content;

import com.jp.elearningjp.entity.content.VocabularySense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabularySenseRepository extends JpaRepository<VocabularySense, Long> {

    List<VocabularySense> findByVocabularyIdAndDeletedFalseOrderBySenseNoAsc(Long vocabId);
}