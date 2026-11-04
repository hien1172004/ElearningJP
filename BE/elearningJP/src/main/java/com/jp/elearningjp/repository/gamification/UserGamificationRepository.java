package com.jp.elearningjp.repository.gamification;

import com.jp.elearningjp.entity.gamification.UserGamification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserGamificationRepository extends JpaRepository<UserGamification, Long> {

    /**
     * Ứng viên cho job STREAK_WARNING — user từng có streak.
     *
     * <p><b>Chỉ dùng làm pre-filter rẻ.</b> Cột {@code current_streak} là bản cache và
     * có thể lệch (chính sách STRICT: chỉ mục MỚI mới giữ streak). Kết luận streak thật
     * phải tính từ {@code user_daily_stats} qua
     * {@code UserDailyStatsRepository.findStreakWindow} +
     * {@code StreakService.computeCurrentStreakFromDesc}.</p>
     */
    @Query("SELECT g.userId FROM UserGamification g WHERE g.currentStreak >= 1")
    List<Long> findUserIdsWithPositiveStreak();

    /**
     * Phân bố streak (ƯỚC TÍNH từ cột cache current_streak) cho dashboard admin:
     * [bucket, số user] với bucket 1 = 1–2 ngày, 2 = 3–6, 3 = 7–29, 4 = 30+.
     * Streak coi là đã đứt nếu hoạt động gần nhất trước {@code yesterday}; các user đó
     * (và user chưa có bản ghi) không có trong kết quả — service tự suy ra nhóm 0.
     */
    @Query(value = """
            SELECT CASE WHEN g.current_streak < 3 THEN 1
                        WHEN g.current_streak < 7 THEN 2
                        WHEN g.current_streak < 30 THEN 3
                        ELSE 4 END AS bucket,
                   COUNT(*)
            FROM user_gamification g
            JOIN users u ON u.id = g.user_id AND u.deleted = false
            WHERE g.current_streak > 0 AND g.last_activity_date >= :yesterday
            GROUP BY bucket
            """, nativeQuery = true)
    List<Object[]> countActiveStreakBuckets(LocalDate yesterday);
}
