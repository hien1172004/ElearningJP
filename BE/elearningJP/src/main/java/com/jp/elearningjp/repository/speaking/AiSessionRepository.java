package com.jp.elearningjp.repository.speaking;

import com.jp.elearningjp.entity.speaking.AiSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiSessionRepository extends JpaRepository<AiSession, Long> {

    Optional<AiSession> findByIdAndUser_IdAndDeletedFalse(Long id, Long userId);

    List<AiSession> findByUser_IdAndDeletedFalseOrderByCreatedAtDesc(Long userId);

    Page<AiSession> findByUser_IdAndDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE AiSession s SET s.deleted = true, s.deletedAt = :now WHERE s.user.id = :userId AND s.deleted = false")
    int softDeleteAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}
