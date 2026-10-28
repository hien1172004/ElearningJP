package com.jp.elearningjp.repository.gamification;

import com.jp.elearningjp.entity.gamification.XpLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface XpLogRepository extends JpaRepository<XpLog, Long> {

    /** Tinh tong XP theo khoang thoi gian — dung cho daily/weekly leaderboard fallback. */
    @Query("""
            SELECT COALESCE(SUM(x.amount), 0)
            FROM XpLog x
            WHERE x.deleted = false
              AND x.user.id = :userId
              AND x.createdAt BETWEEN :from AND :to
            """)
    Long sumAmountByUserAndRange(Long userId, Instant from, Instant to);

    /** Lich su XP gan nhat cua user. */
    List<XpLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}