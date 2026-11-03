package com.jp.elearningjp.repository.notebook;

import com.jp.elearningjp.entity.notebook.NotebookView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotebookViewRepository extends JpaRepository<NotebookView, Long> {

    Optional<NotebookView> findByNotebookIdAndUserId(Long notebookId, Long userId);

    @Query("SELECT nv.notebook.id FROM NotebookView nv WHERE nv.user.id = :userId ORDER BY nv.viewedAt DESC")
    Page<Long> findRecentlyViewedNotebookIdsByUserId(@Param("userId") Long userId, Pageable pageable);
}

