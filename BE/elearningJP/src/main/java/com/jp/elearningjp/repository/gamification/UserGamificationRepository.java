package com.jp.elearningjp.repository.gamification;

import com.jp.elearningjp.entity.gamification.UserGamification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}
