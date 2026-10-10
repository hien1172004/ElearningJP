package com.jp.elearningjp.repository.content;

import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.springframework.data.domain.Page;
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
     * Tra cứu phân trang với bộ lọc JLPT và sắp xếp ưu tiên chính xác (Relevance Sorting):
     * 1. Khớp chính xác (word/hiragana/romaji)
     * 2. Bắt đầu bằng từ khóa (Prefix match)
     * 3. Khớp chuẩn âm Hán Việt
     * 4. Khớp chứa từ khóa (Substring)
     */
    @Query(value = "SELECT v FROM Vocabulary v WHERE v.deleted = false AND v.active = true AND " +
                   "(:jlptLevel IS NULL OR v.jlptLevel = :jlptLevel) AND (" +
                   "LOWER(v.word) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "LOWER(v.hiragana) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "LOWER(v.romaji) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "LOWER(v.hanViet) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "LOWER(v.meaningVi) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                   "ORDER BY " +
                   "CASE " +
                   "  WHEN LOWER(v.word) = LOWER(:keyword) OR LOWER(v.hiragana) = LOWER(:keyword) OR LOWER(v.romaji) = LOWER(:keyword) THEN 1 " +
                   "  WHEN LOWER(v.word) LIKE LOWER(CONCAT(:keyword, '%')) OR LOWER(v.hiragana) LIKE LOWER(CONCAT(:keyword, '%')) THEN 2 " +
                   "  WHEN LOWER(v.hanViet) = LOWER(:keyword) THEN 3 " +
                   "  ELSE 4 " +
                   "END ASC, v.id ASC",
           countQuery = "SELECT COUNT(v) FROM Vocabulary v WHERE v.deleted = false AND v.active = true AND " +
                        "(:jlptLevel IS NULL OR v.jlptLevel = :jlptLevel) AND (" +
                        "LOWER(v.word) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(v.hiragana) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(v.romaji) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(v.hanViet) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(v.meaningVi) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Vocabulary> searchWithFilter(@Param("keyword") String keyword,
                                      @Param("jlptLevel") JlptLevel jlptLevel,
                                      Pageable pageable);

    /**
     * Tra cứu chính xác cho popup bôi đen (exact match theo word hoặc hiragana).
     */
    @Query("SELECT v FROM Vocabulary v WHERE v.deleted = false AND v.active = true AND (" +
           "v.word = :text OR v.hiragana = :text)")
    List<Vocabulary> findExactMatches(@Param("text") String text);
}
