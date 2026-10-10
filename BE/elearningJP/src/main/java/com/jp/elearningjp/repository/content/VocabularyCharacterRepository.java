package com.jp.elearningjp.repository.content;

import com.jp.elearningjp.entity.content.VocabularyCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabularyCharacterRepository extends JpaRepository<VocabularyCharacter, Long> {

    /**
     * Lấy các từ vựng chứa chữ Hán tương ứng, kèm theo âm đọc cụ thể (Reading) và loại âm (Onyomi, Kunyomi).
     */
    @Query("SELECT vc FROM VocabularyCharacter vc " +
           "JOIN FETCH vc.vocabulary v " +
           "WHERE vc.character.character = :character " +
           "AND vc.deleted = false " +
           "AND v.deleted = false " +
           "AND v.active = true " +
           "ORDER BY vc.readingType ASC, v.jlptLevel DESC")
    List<VocabularyCharacter> findRelatedWordsByCharacter(@Param("character") String character);

    /**
     * Lấy danh sách các chữ Hán cấu thành nên từ vựng này.
     */
    @Query("SELECT vc FROM VocabularyCharacter vc " +
           "JOIN FETCH vc.character c " +
           "WHERE vc.vocabulary.id = :vocabId " +
           "AND vc.deleted = false " +
           "AND c.deleted = false " +
           "ORDER BY vc.position ASC")
    List<VocabularyCharacter> findCharactersByVocabId(@Param("vocabId") Long vocabId);
}
