package com.jp.elearningjp.repository.content;

import com.jp.elearningjp.entity.content.WritingCharacter;
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

    /**
     * Tra cứu Hán tự theo ký tự hoặc âm Hán Việt.
     */
    @Query("SELECT wc FROM WritingCharacter wc WHERE wc.deleted = false AND wc.active = true AND (" +
           "wc.character = :keyword OR " +
           "LOWER(wc.hanViet) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(wc.meaningVi) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<WritingCharacter> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
