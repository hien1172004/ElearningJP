package com.jp.elearningjp.service.gamification;

import com.jp.elearningjp.entity.gamification.UserDailyStats;
import com.jp.elearningjp.entity.gamification.UserDailyStatsId;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.repository.gamification.UserDailyStatsRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.config.AppConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Quan ly {@code user_daily_stats} — bang tong hop theo ngay.
 *
 * <p>Chay {@code REQUIRES_NEW} de moi thay doi daily stats doc lap voi
 * transaction goc (tranh bi rollback khi phan hoat dong that bai).</p>
 *
 * <p>Co native {@code INSERT ... ON CONFLICT DO UPDATE WHERE} de tranh race condition
 * khi user tra tu dong thoi (nhieu tab / nhieu request).</p>
 *
 * <p><b>Cau hinh tu app_config:</b></p>
 * <ul>
 *   <li>{@code lookup_ceiling}: so lookup toi da tinh diem trong ngay (default 20)</li>
 *   <li>{@code lookup_xp}: XP nhan khi tra tu (default 2)</li>
 *   <li>{@code lesson_xp}: XP nhan khi hoan thanh bai hoc (default 10)</li>
 *   <li>{@code min_study_seconds}: so giay toi thieu de goal_met (default 300)</li>
 * </ul>
 *
 * <p><b>goal_met logic:</b> ngay co hoat dong neu thoa man bat ky:
 * <ul>
 *   <li>{@code lookups >= goalWords} (tra tu)</li>
 *   <li>{@code lessons_completed > 0} (hoan thanh bai hoc)</li>
 *   <li>{@code study_seconds >= min_study_seconds} (5 phut hoc)</li>
 * </ul></p>
 */
@Service
@RequiredArgsConstructor
public class DailyStatsService {

    /** Muc tieu mac dinh 5 tu/ngay. */
    public static final int DEFAULT_GOAL_WORDS = 5;

    /** So phut hoc toi thieu de goal_met = true (5 phut = 300 giay). */
    public static final int DEFAULT_MIN_STUDY_SECONDS = 300;

    /** So lookup toi da trong ngay (mac dinh). */
    public static final int DEFAULT_LOOKUP_CEILING = 20;

    /** So lookup toi da trong ngay (key trong app_config). */
    private static final String CFG_LOOKUP_CEILING = "lookup_ceiling";

    /** So giay toi thieu de goal_met (key trong app_config). */
    private static final String CFG_MIN_STUDY_SECONDS = "min_study_seconds";

    private final UserDailyStatsRepository dailyStatsRepository;
    private final UserRepository userRepository;
    private final AppConfigService appConfigService;

    /** Múi giờ nghiệp vụ — xem {@link com.jp.elearningjp.config.TimeConfig}. */
    private final Clock clock;

    // -------------------------------------------------------------------------
    // Tra tu dien
    // -------------------------------------------------------------------------

    /**
     * Ghi 1 lan tra tu dien vao daily stats.
     *
     * <p>Su dung upsert nguyen tu: chi tang lookups_scored khi chua vuot tran.
     * Neu vuot tran, lookups van tang nhung lookups_scored khong doi.</p>
     *
     * @param userId user tra tu
     * @param xpEarned XP nhan duoc (thuong = 2)
     * @return LookupResult chua daily stats va flag scored (true = duoc tinh diem)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LookupResult recordLookup(Long userId, int xpEarned) {
        LocalDate today = LocalDate.now(clock);

        int lookupCeiling = appConfigService.getIntValue(CFG_LOOKUP_CEILING, DEFAULT_LOOKUP_CEILING);
        int minStudySeconds = appConfigService.getIntValue(CFG_MIN_STUDY_SECONDS, DEFAULT_MIN_STUDY_SECONDS);

        // Doc lookups_scored hien tai de biet lan nay co duoc tinh diem hay khong
        int scoredBefore = dailyStatsRepository.findByIdUserIdAndIdStatDate(userId, today)
                .map(UserDailyStats::getLookupsScored)
                .orElse(0);

        // Upsert nguyen tu - tang lookups (luon), lookups_scored chi tang khi chua vuot tran
        dailyStatsRepository.upsertLookup(
                userId, today, xpEarned, lookupCeiling, minStudySeconds, Instant.now());

        // scored = lookups_scored da tang sau upsert
        int scoredAfter = dailyStatsRepository.findByIdUserIdAndIdStatDate(userId, today)
                .map(UserDailyStats::getLookupsScored)
                .orElse(0);

        boolean scored = scoredAfter > scoredBefore;

        // Lay row hien tai de tra ve
        UserDailyStats stats = dailyStatsRepository.findByIdUserIdAndIdStatDate(userId, today)
                .orElseGet(() -> createEmpty(userId, today));

        return new LookupResult(stats, scored);
    }

    /**
     * Ket qua cua recordLookup.
     *
     * @param stats daily stats hien tai
     * @param scored true neu lan tra nay duoc tinh diem (chua vuot tran)
     */
    public record LookupResult(UserDailyStats stats, boolean scored) {}

    // -------------------------------------------------------------------------
    // Hoan thanh bai hoc
    // -------------------------------------------------------------------------

    /**
     * Ghi hoan thanh bai hoc vao daily stats (chi lan dau tien).
     *
     * @param userId user hoan thanh bai
     * @param xpEarned XP nhan duoc (thuong = 10)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLessonComplete(Long userId, int xpEarned) {
        int minStudySeconds = appConfigService.getIntValue(CFG_MIN_STUDY_SECONDS, DEFAULT_MIN_STUDY_SECONDS);
        dailyStatsRepository.incrementLessonsCompleted(userId, LocalDate.now(clock), xpEarned, minStudySeconds);
    }

    // -------------------------------------------------------------------------
    // Thoi gian hoc (heartbeat)
    // -------------------------------------------------------------------------

    /** Cong them thoi gian hoc (heartbeat). Tu dong re-evaluate goal_met. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addStudySeconds(Long userId, LocalDate date, int seconds, int xpEarned) {
        if (seconds <= 0 && xpEarned <= 0) {
            return;
        }
        int minStudySeconds = appConfigService.getIntValue(CFG_MIN_STUDY_SECONDS, DEFAULT_MIN_STUDY_SECONDS);
        dailyStatsRepository.addStudySeconds(userId, date, seconds, xpEarned, minStudySeconds);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserDailyStats createEmpty(Long userId, LocalDate date) {
        User user = userRepository.findById(userId).orElse(null);
        return UserDailyStats.builder()
                .id(new UserDailyStatsId(userId, date))
                .user(user)
                .goalWords(DEFAULT_GOAL_WORDS)
                .build();
    }

    /** Lay daily stats hom nay, khong tao moi. */
    @Transactional(readOnly = true)
    public UserDailyStats getOrNull(Long userId, LocalDate date) {
        return dailyStatsRepository.findByIdUserIdAndIdStatDate(userId, date).orElse(null);
    }
}
