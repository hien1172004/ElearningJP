package com.jp.elearningjp.repository.notebook;

import com.jp.elearningjp.entity.notebook.NotebookGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotebookGroupRepository extends JpaRepository<NotebookGroup, Long> {

    @EntityGraph(attributePaths = {"notebooks"})
    List<NotebookGroup> findByUserIdAndDeletedFalseOrderByDisplayOrderAsc(Long userId);

    Optional<NotebookGroup> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    boolean existsByUserIdAndNameAndDeletedFalse(Long userId, String name);
}

