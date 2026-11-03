package com.jp.elearningjp.repository.notebook;

import com.jp.elearningjp.entity.notebook.NotebookWord;
import com.jp.elearningjp.shared.enums.MemoryLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Query("SELECT w FROM NotebookWord w WHERE w.notebook.id = :notebookId AND w.deleted = false AND (w.nextReviewAt IS NULL OR w.nextReviewAt <= :now) ORDER BY w.nextReviewAt ASC NULLS FIRST")
    List<NotebookWord> findDueWordsByNotebookId(@Param("notebookId") Long notebookId, @Param("now") java.time.Instant now, Pageable pageable);

    @Query("SELECT w FROM NotebookWord w WHERE w.notebook.user.id = :userId AND w.deleted = false AND (w.nextReviewAt IS NULL OR w.nextReviewAt <= :now) ORDER BY w.nextReviewAt ASC NULLS FIRST")
    List<NotebookWord> findDueWordsByUserId(@Param("userId") Long userId, @Param("now") java.time.Instant now, Pageable pageable);

    @Query("SELECT w FROM NotebookWord w WHERE w.notebook.user.id = :userId AND w.deleted = false ORDER BY w.displayOrder ASC, w.id ASC")
    List<NotebookWord> findByUserIdAndDeletedFalseOrderByDisplayOrderAscIdAsc(@Param("userId") Long userId);
}

