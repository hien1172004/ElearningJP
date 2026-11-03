package com.jp.elearningjp.repository.notebook;

import com.jp.elearningjp.entity.notebook.Notebook;
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
public interface NotebookRepository extends JpaRepository<Notebook, Long>, JpaSpecificationExecutor<Notebook> {

    Optional<Notebook> findByIdAndDeletedFalse(Long id);

    Optional<Notebook> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    Optional<Notebook> findByShareCodeAndDeletedFalse(String shareCode);

    boolean existsByShareCode(String shareCode);

    Optional<Notebook> findFirstByUserIdAndSourceNotebookIdAndDeletedFalseOrderByIdDesc(Long userId, Long sourceNotebookId);

    boolean existsByUserIdAndSourceNotebookIdAndDeletedFalse(Long userId, Long sourceNotebookId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"sourceNotebook"})
    List<Notebook> findByUserIdAndSourceNotebookIdInAndDeletedFalse(Long userId, List<Long> sourceNotebookIds);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "group"})
    List<Notebook> findByUserIdAndDeletedFalseOrderByIdDesc(Long userId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "group"})
    List<Notebook> findByUserIdAndGroupIdAndDeletedFalseOrderByIdDesc(Long userId, Long groupId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "group"})
    @Query("SELECT n FROM Notebook n WHERE n.user.id = :userId AND n.group IS NULL AND n.deleted = false ORDER BY n.id DESC")
    List<Notebook> findUngroupedNotebooks(@Param("userId") Long userId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "group"})
    Page<Notebook> findByIsPublicTrueAndDeletedFalseAndWordCountGreaterThan(int minWordCount, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "group"})
    Page<Notebook> findAll(org.springframework.data.jpa.domain.Specification<Notebook> spec, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "group"})
    List<Notebook> findByIdInAndDeletedFalse(List<Long> ids);
}
