package com.jp.elearningjp.repository.content;

import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.shared.enums.CharacterType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WritingCharacterRepository extends JpaRepository<WritingCharacter, Long> {

    Optional<WritingCharacter> findByIdAndDeletedFalse(Long id);

    Optional<WritingCharacter> findByCharacterAndDeletedFalse(String character);

    List<WritingCharacter> findByCharacterInAndDeletedFalse(Collection<String> characters);

    List<WritingCharacter> findByCharTypeAndDeletedFalseAndActiveTrue(CharacterType charType);

    /**
     * Tra cứu nhanh Hán tự theo ký tự, âm Hán Việt, romaji hoặc nghĩa tiếng Việt.
     */
    @Query("SELECT wc FROM WritingCharacter wc WHERE wc.deleted = false AND wc.active = true AND " +
           "wc.charType = com.jp.elearningjp.shared.enums.CharacterType.KANJI AND (" +
           "wc.character = :keyword OR " +
           "LOWER(wc.hanViet) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(wc.romaji) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(wc.meaningVi) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<WritingCharacter> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Tra cứu Hán tự phân trang với bộ lọc JLPT và sắp xếp ưu tiên chính xác:
     * 1. Khớp chính xác ký tự (character)
     * 2. Khớp chuẩn âm Hán Việt hoặc romaji
     * 3. Bắt đầu bằng âm Hán Việt hoặc romaji
     * 4. Khớp nghĩa
     */
    @Query(value = "SELECT wc FROM WritingCharacter wc WHERE wc.deleted = false AND wc.active = true AND " +
                   "wc.charType = com.jp.elearningjp.shared.enums.CharacterType.KANJI AND " +
                   "(:jlptLevel IS NULL OR wc.jlptLevel = :jlptLevel) AND (" +
                   "wc.character = :keyword OR " +
                   "LOWER(wc.hanViet) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "LOWER(wc.romaji) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "LOWER(wc.meaningVi) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                   "ORDER BY " +
                   "CASE " +
                   "  WHEN wc.character = :keyword THEN 1 " +
                   "  WHEN LOWER(wc.hanViet) = LOWER(:keyword) OR LOWER(wc.romaji) = LOWER(:keyword) THEN 2 " +
                   "  WHEN LOWER(wc.hanViet) LIKE LOWER(CONCAT(:keyword, '%')) OR LOWER(wc.romaji) LIKE LOWER(CONCAT(:keyword, '%')) THEN 3 " +
                   "  ELSE 4 " +
                   "END ASC, wc.id ASC",
           countQuery = "SELECT COUNT(wc) FROM WritingCharacter wc WHERE wc.deleted = false AND wc.active = true AND " +
                        "wc.charType = com.jp.elearningjp.shared.enums.CharacterType.KANJI AND " +
                        "(:jlptLevel IS NULL OR wc.jlptLevel = :jlptLevel) AND (" +
                        "wc.character = :keyword OR " +
                        "LOWER(wc.hanViet) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(wc.romaji) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(wc.meaningVi) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<WritingCharacter> searchKanjiWithFilter(@Param("keyword") String keyword,
                                                 @Param("jlptLevel") JlptLevel jlptLevel,
                                                 Pageable pageable);
}
