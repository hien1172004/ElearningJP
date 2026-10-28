package com.jp.elearningjp.service.progress;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jp.elearningjp.repository.gamification.UserDailyStatsRepository;
import com.jp.elearningjp.service.config.AppConfigService;

import java.time.LocalDate;

/**
 * Đọc điểm xếp hạng tuyệt đối của 1 user trong 1 tuần từ DB.
 *
 * <p><b>Vì sao tách riêng?</b> {@link LeaderboardService#syncUserScore} được gọi
 * trong {@code afterCommit} — lúc đó transaction gốc đã commit nhưng connection
 * vẫn còn bind trên thread. Để chắc chắn đọc được trạng thái <b>đã commit mới
 * nhất</b> (không dính snapshot cũ), query đọc score phải chạy trong
 * {@code REQUIRES_NEW} → connection mới, snapshot mới.</p>
 *
 * <p>Postgres mặc định là READ COMMITTED nên thường đã đúng, nhưng
 * {@code REQUIRES_NEW} làm rõ ý định và chống lỗi khi ai đó đổi isolation level.</p>
 */
@Component
@RequiredArgsConstructor
public class LeaderboardScoreReader {

    /** App config key cho điểm mỗi từ MỚI đã học. */
    private static final String CFG_LEARNED_ITEM_POINTS = "learned_item_points";
    /** App config key cho điểm mỗi bài học hoàn thành. */
    private static final String CFG_LESSON_POINTS = "lesson_points";

    private static final int DEFAULT_LEARNED_ITEM_POINTS = 1;
    private static final int DEFAULT_LESSON_POINTS = 1;

    private final UserDailyStatsRepository dailyStatsRepository;
    private final AppConfigService appConfigService;

    /**
     * Đọc điểm xếp hạng của user trong tuần.
     *
     * <p>Điểm = {@code new_items_learned * learned_item_points + lessons_completed * lesson_points}.</p>
     *
     * @param userId    user cần đọc
     * @param weekStart thứ 2 của tuần
     * @param weekEnd   chủ nhật của tuần
     * @return điểm (0 nếu user chưa có hoạt động trong tuần)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public long readWeekScore(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        int learnedItemPoints = appConfigService.getIntValue(CFG_LEARNED_ITEM_POINTS, DEFAULT_LEARNED_ITEM_POINTS);
        int lessonPoints = appConfigService.getIntValue(CFG_LESSON_POINTS, DEFAULT_LESSON_POINTS);
        return dailyStatsRepository.findUserWeekScore(
                userId, weekStart, weekEnd, learnedItemPoints, lessonPoints);
    }
}
