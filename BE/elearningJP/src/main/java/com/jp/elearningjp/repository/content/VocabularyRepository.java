package com.jp.elearningjp.repository.content;

import com.jp.elearningjp.entity.content.Vocabulary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {

    Optional<Vocabulary> findByIdAndDeletedFalse(Long id);

    Optional<Vocabulary> findByWordAndDeletedFalse(String word);

    Optional<Vocabulary> findByHiraganaAndDeletedFalse(String hiragana);

    /**
     * Tra cứu nhanh từ vựng theo từ viết, cách đọc, romaji, hán việt hoặc nghĩa.
     */
    @Query("SELECT v FROM Vocabulary v WHERE v.deleted = false AND v.active = true AND (" +
           "LOWER(v.word) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(v.hiragana) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(v.romaji) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(v.hanViet) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(v.meaningVi) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Vocabulary> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Tra cứu chính xác cho popup bôi đen (exact match theo word hoặc hiragana).
     */
    @Query("SELECT v FROM Vocabulary v WHERE v.deleted = false AND v.active = true AND (" +
           "v.word = :text OR v.hiragana = :text)")
    List<Vocabulary> findExactMatches(@Param("text") String text);
}
