package com.jp.elearningjp.repository.notebook;

import com.jp.elearningjp.entity.notebook.NotebookWord;
import com.jp.elearningjp.shared.enums.MemoryLevel;
import com.jp.elearningjp.shared.enums.NotebookItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotebookWordRepository extends JpaRepository<NotebookWord, Long>, JpaSpecificationExecutor<NotebookWord> {

    Optional<NotebookWord> findByIdAndDeletedFalse(Long id);

    List<NotebookWord> findByNotebookIdAndDeletedFalseOrderByDisplayOrderAscIdAsc(Long notebookId);

    Page<NotebookWord> findByNotebookIdAndDeletedFalse(Long notebookId, Pageable pageable);

    long countByNotebookIdAndDeletedFalse(Long notebookId);

    long countByNotebookIdAndMemoryLevelAndDeletedFalse(Long notebookId, MemoryLevel memoryLevel);

    @Query("SELECT COUNT(w) FROM NotebookWord w WHERE w.notebook.user.id = :userId AND w.deleted = false")
    long countTotalWordsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(w) FROM NotebookWord w WHERE w.notebook.user.id = :userId AND w.memoryLevel = :level AND w.deleted = false")
    long countWordsByUserIdAndMemoryLevel(@Param("userId") Long userId, @Param("level") MemoryLevel level);

    @Query(value = "SELECT * FROM notebook_words WHERE notebook_id = :notebookId AND deleted = false ORDER BY RANDOM()", nativeQuery = true)
    List<NotebookWord> findShuffledWordsByNotebookId(@Param("notebookId") Long notebookId);

    /** Thẻ đến hạn ôn — nạp sẵn chữ Hán / ngữ pháp (LEFT JOIN) để màn ôn tập không bị N+1. */
    @EntityGraph(attributePaths = {"character", "grammar"})
    @Query("SELECT w FROM NotebookWord w WHERE w.notebook.id = :notebookId AND w.deleted = false AND w.itemType IN :types AND (w.nextReviewAt IS NULL OR w.nextReviewAt <= :now) ORDER BY w.nextReviewAt ASC NULLS FIRST")
    List<NotebookWord> findDueWordsByNotebookId(@Param("notebookId") Long notebookId, @Param("types") Collection<NotebookItemType> types,
                                                @Param("now") java.time.Instant now, Pageable pageable);

    /** Thẻ đến hạn ôn của mọi sổ tay của user — nạp sẵn chữ Hán / ngữ pháp (LEFT JOIN) để tránh N+1. */
    @EntityGraph(attributePaths = {"character", "grammar"})
    @Query("SELECT w FROM NotebookWord w WHERE w.notebook.user.id = :userId AND w.deleted = false AND w.itemType IN :types AND (w.nextReviewAt IS NULL OR w.nextReviewAt <= :now) ORDER BY w.nextReviewAt ASC NULLS FIRST")
    List<NotebookWord> findDueWordsByUserId(@Param("userId") Long userId, @Param("types") Collection<NotebookItemType> types,
                                            @Param("now") java.time.Instant now, Pageable pageable);

    @Query("SELECT w FROM NotebookWord w WHERE w.notebook.user.id = :userId AND w.deleted = false AND w.itemType IN :types ORDER BY w.displayOrder ASC, w.id ASC")
    List<NotebookWord> findPoolByUserId(@Param("userId") Long userId, @Param("types") Collection<NotebookItemType> types);

    @Query("SELECT w FROM NotebookWord w WHERE w.notebook.id = :notebookId AND w.deleted = false AND w.itemType IN :types ORDER BY w.displayOrder ASC, w.id ASC")
    List<NotebookWord> findPoolByNotebookId(@Param("notebookId") Long notebookId, @Param("types") Collection<NotebookItemType> types);

    // ===== V59: sổ tay nhiều loại thẻ =====

    boolean existsByNotebook_IdAndItemTypeAndVocabulary_Id(Long notebookId, NotebookItemType itemType, Long vocabularyId);

    boolean existsByNotebook_IdAndItemTypeAndCharacter_Id(Long notebookId, NotebookItemType itemType, Long characterId);

    boolean existsByNotebook_IdAndItemTypeAndGrammar_Id(Long notebookId, NotebookItemType itemType, Long grammarId);

    /** Danh sách phân trang (tìm kiếm) — nạp sẵn chữ Hán / ngữ pháp / bài nguồn để map response không bị N+1. */
    @Override
    @EntityGraph(attributePaths = {"character", "grammar", "sourceLesson"})
    Page<NotebookWord> findAll(Specification<NotebookWord> spec, Pageable pageable);

    /** Đếm thẻ theo loại trong 1 sổ: mỗi dòng = [NotebookItemType, Long]. */
    @Query("SELECT w.itemType, COUNT(w) FROM NotebookWord w WHERE w.notebook.id = :notebookId AND w.deleted = false GROUP BY w.itemType")
    List<Object[]> countByTypeInNotebook(@Param("notebookId") Long notebookId);

    @Query("SELECT w.vocabulary.id FROM NotebookWord w WHERE w.notebook.id = :notebookId AND w.deleted = false AND w.itemType = com.jp.elearningjp.shared.enums.NotebookItemType.VOCAB AND w.vocabulary IS NOT NULL")
    List<Long> findVocabularyIdsInNotebook(@Param("notebookId") Long notebookId);

    @Query("SELECT w.character.id FROM NotebookWord w WHERE w.notebook.id = :notebookId AND w.deleted = false AND w.itemType = com.jp.elearningjp.shared.enums.NotebookItemType.KANJI AND w.character IS NOT NULL")
    List<Long> findCharacterIdsInNotebook(@Param("notebookId") Long notebookId);

    @Query("SELECT w.grammar.id FROM NotebookWord w WHERE w.notebook.id = :notebookId AND w.deleted = false AND w.itemType = com.jp.elearningjp.shared.enums.NotebookItemType.GRAMMAR AND w.grammar IS NOT NULL")
    List<Long> findGrammarIdsInNotebook(@Param("notebookId") Long notebookId);

    @Query("SELECT DISTINCT w.vocabulary.id FROM NotebookWord w WHERE w.notebook.user.id = :userId AND w.notebook.deleted = false AND w.deleted = false AND w.itemType = com.jp.elearningjp.shared.enums.NotebookItemType.VOCAB AND w.vocabulary IS NOT NULL")
    List<Long> findSavedVocabularyIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT w.character.id FROM NotebookWord w WHERE w.notebook.user.id = :userId AND w.notebook.deleted = false AND w.deleted = false AND w.itemType = com.jp.elearningjp.shared.enums.NotebookItemType.KANJI AND w.character IS NOT NULL")
    List<Long> findSavedCharacterIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT w.grammar.id FROM NotebookWord w WHERE w.notebook.user.id = :userId AND w.notebook.deleted = false AND w.deleted = false AND w.itemType = com.jp.elearningjp.shared.enums.NotebookItemType.GRAMMAR AND w.grammar IS NOT NULL")
    List<Long> findSavedGrammarIdsByUserId(@Param("userId") Long userId);
}

