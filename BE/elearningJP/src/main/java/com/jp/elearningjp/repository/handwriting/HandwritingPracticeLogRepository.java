package com.jp.elearningjp.repository.handwriting;

import com.jp.elearningjp.entity.handwriting.HandwritingPracticeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HandwritingPracticeLogRepository extends JpaRepository<HandwritingPracticeLog, Long> {

    /**
     * Toàn bộ lần luyện của user cho 1 chữ (mới nhất trước).
     * lessonId null = chỉ lấy lần luyện ngoài bài học (từ điển / sổ tay).
     */
    @Query("""
            SELECT l FROM HandwritingPracticeLog l LEFT JOIN l.lesson ls
            WHERE l.user.id = :userId AND l.character = :character AND l.deleted = false
              AND ((:lessonId IS NULL AND ls IS NULL) OR ls.id = :lessonId)
            ORDER BY l.createdAt DESC
            """)
    List<HandwritingPracticeLog> findByUserAndCharacter(@Param("userId") Long userId,
                                                        @Param("character") String character,
                                                        @Param("lessonId") Long lessonId);
}
