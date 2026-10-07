package com.jp.elearningjp.repository.content;

import com.jp.elearningjp.entity.content.Vocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {
    Optional<Vocabulary> findByIdAndDeletedFalse(Long id);
}
