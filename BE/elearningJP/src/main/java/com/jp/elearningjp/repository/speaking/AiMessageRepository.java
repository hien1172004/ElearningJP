package com.jp.elearningjp.repository.speaking;

import com.jp.elearningjp.entity.speaking.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    List<AiMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    @Query(value = "SELECT * FROM ai_message WHERE session_id = :sessionId ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<AiMessage> findRecentMessagesBySessionId(@Param("sessionId") Long sessionId, @Param("limit") int limit);

    long countBySessionId(Long sessionId);
}
