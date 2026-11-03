package com.jp.elearningjp.repository.notebook;

import com.jp.elearningjp.entity.notebook.NotebookLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotebookLikeRepository extends JpaRepository<NotebookLike, Long> {

    boolean existsByNotebookIdAndUserId(Long notebookId, Long userId);

    Optional<NotebookLike> findByNotebookIdAndUserId(Long notebookId, Long userId);

    void deleteByNotebookIdAndUserId(Long notebookId, Long userId);

    long countByNotebookId(Long notebookId);

    @Query("SELECT nl.notebook.id FROM NotebookLike nl WHERE nl.user.id = :userId ORDER BY nl.createdAt DESC")
    Page<Long> findLikedNotebookIdsByUserId(@Param("userId") Long userId, Pageable pageable);
}

