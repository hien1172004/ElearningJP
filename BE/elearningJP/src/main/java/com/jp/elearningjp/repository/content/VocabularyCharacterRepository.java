package com.jp.elearningjp.repository.content;

import com.jp.elearningjp.entity.content.Vocabulary;
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

    /**
     * Lấy danh sách {@link Vocabulary} (đã JOIN FETCH) chứa chữ Hán có id = {@code characterId}.
     * Dùng cho {@code LessonKanjiItem}: related vocab của 1 chữ Hán = mọi từ vựng trong từ điển
     * có liên kết {@code vocabulary_characters} với {@code writing_character} này.
     *
     * <p>Sắp xếp: ưu tiên JLPT (N5 → N1), tie-break theo id tăng dần.</p>
     */
    @Query("SELECT DISTINCT v FROM VocabularyCharacter vc " +
           "JOIN vc.vocabulary v " +
           "WHERE vc.character.id = :characterId " +
           "AND vc.deleted = false " +
           "AND v.deleted = false " +
           "AND v.active = true " +
           "ORDER BY v.jlptLevel DESC, v.id ASC")
    List<Vocabulary> findRelatedVocabulariesByCharacterId(@Param("characterId") Long characterId);

    /**
     * Batch: lấy danh sách {@link Vocabulary} chứa bất kỳ chữ Hán nào trong tập {@code characterIds}.
     * Trả về map kết quả dạng {@code Map<Long characterId, List<Vocabulary>>}.
     *
     * <p>Dùng khi build response của nhiều {@code LessonKanjiItem} cùng lúc để giảm N+1 query.</p>
     */
    @Query("SELECT vc.character.id AS characterId, v " +
           "FROM VocabularyCharacter vc " +
           "JOIN vc.vocabulary v " +
           "WHERE vc.character.id IN :characterIds " +
           "AND vc.deleted = false " +
           "AND v.deleted = false " +
           "AND v.active = true " +
           "ORDER BY vc.character.id, v.jlptLevel DESC, v.id ASC")
    List<CharacterIdVocabularyProjection> findRelatedVocabulariesByCharacterIds(
            @Param("characterIds") List<Long> characterIds);

    /**
     * Projection interface cho query batch ở trên: trả về cặp (characterId, vocabulary).
     */
    interface CharacterIdVocabularyProjection {
        Long getCharacterId();
        Vocabulary getV();
    }
}