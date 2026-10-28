package com.jp.elearningjp.service.gamification;

import com.jp.elearningjp.entity.gamification.UserGamification;
import com.jp.elearningjp.entity.gamification.XpLog;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.repository.gamification.UserGamificationRepository;
import com.jp.elearningjp.repository.gamification.XpLogRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.config.AppConfigService;
import com.jp.elearningjp.service.progress.LeaderboardService;
import com.jp.elearningjp.service.progress.LeaderboardWeekUtil;
import com.jp.elearningjp.shared.enums.XpSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;

/**
 * Cong XP + cap nhat streak + dong bo Redis Sorted Set (bang xep hang).
 *
 * <p>XP duoc luu o 2 noi:</p>
 * <ol>
 *   <li><b>DB</b> ({@code user_gamification.total_xp} + {@code xp_logs}) — nguon su that</li>
 *   <li><b>Redis Sorted Set</b> ({@code leaderboard:weekly:YYYY-Www}) — bang xep hang tuan
 *       theo tong XP, cap nhat dong moi khi {@link #awardXp} được gọi</li>
 * </ol>
 *
 * <p>Redis Sorted Set la primary storage cho leaderboard (khong phai cache).
 * Neu Redis fail, leaderboard fallback sang query DB.</p>
 *
 * <p><b>ZINCRBY sau commit:</b> ZINCRBY chi duoc goi sau khi transaction commit thanh cong,
 * bao try/catch de Redis loi khong anh huong den luong chinh.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class XpService {

    /** Muc tieu mac dinh 5 tu/ngay. */
    public static final int DEFAULT_GOAL_WORDS = 5;

    /** So diem mac dinh khi tra tu (neu app_config chua cau hinh). */
    public static final int DEFAULT_XP_PER_LOOKUP = 2;

    /** Diem cong mac dinh vao leaderboard khi hoc 1 tu MOI (neu app_config chua cau hinh). */
    public static final int DEFAULT_LEARNED_ITEM_POINTS = 1;

    /** Diem cong mac dinh vao leaderboard khi hoan thanh bai hoc. */
    public static final int DEFAULT_LESSON_POINTS = 1;

    /** So XP mac dinh khi hoan thanh bai hoc. */
    public static final int DEFAULT_XP_PER_LESSON = 10;

    /** App config keys */
    private static final String CFG_LOOKUP_XP = "lookup_xp";
    private static final String CFG_LESSON_XP = "lesson_xp";
    private static final String CFG_LEARNED_ITEM_POINTS = "learned_item_points";
    private static final String CFG_LESSON_POINTS = "lesson_points";

    private final UserGamificationRepository gamificationRepository;
    private final XpLogRepository xpLogRepository;
    private final UserRepository userRepository;
    private final StreakService streakService;
    private final LeaderboardService leaderboardService;
    private final AppConfigService appConfigService;

    /** Múi giờ nghiệp vụ — xem {@link com.jp.elearningjp.config.TimeConfig}. */
    private final Clock clock;

    // -------------------------------------------------------------------------
    // XP
    // -------------------------------------------------------------------------

    /**
     * Cong XP cho user.
     *
     * <p>Dong thoi cung cap diem leaderboard (LookupResult.scored dam bao chi cong khi can thiet).</p>
     *
     * <p><b>ZINCRBY sau commit:</b> Redis chi duoc goi sau khi transaction commit thanh cong.
     * Neu Redis loi, chi log warn, khong anh huong luong chinh.</p>
     *
     * @param userId   user nhan XP
     * @param amount   so XP cong them (phai > 0)
     * @param source   nguon XP (luu vao {@code xp_logs})
     * @param sourceRefId id tham chieu (vd: lessonId) — co the null
     */
    @Transactional
    public void awardXp(Long userId, int amount, XpSource source, Long sourceRefId) {
        if (amount <= 0) {
            return;
        }
        UserGamification gamification = getOrCreate(userId);
        gamification.setTotalXp(gamification.getTotalXp() + amount);
        gamificationRepository.save(gamification);

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            xpLogRepository.save(XpLog.builder()
                    .user(user)
                    .amount(amount)
                    .sourceType(source)
                    .sourceRefId(sourceRefId)
                    .build());
        }

        // Dong bo Redis Sorted Set sau khi transaction commit.
        //
        // <b>syncUserScore</b> (khong phai incrementScore): doc diem tuyet doi cua
        // user trong tuan tu DB roi ZADD GT. Idempotent + khong ghi lui diem.
        // Chi goi cho LESSON_COMPLETE (diem bai hoc). Luong tra tu dong bo rieng
        // o StudyTrackerService khi luot tra la tu MOI (xem leaderboardPoints).
        if (leaderboardPoints(source) > 0) {
            final String weekKey = LeaderboardWeekUtil.currentWeekKey(clock);
            registerAfterCommit(() -> leaderboardService.syncUserScore(userId, weekKey));
        }
    }

    /**
     * Tra ve diem cong vao leaderboard tu XpSource.
     *
     * <p>Chi {@code LESSON_COMPLETE} tu dong trigger sync Redis (diem bai hoc).
     * {@code DAILY_TASK} (tra tu) <b>khong</b> trigger o day: diem leaderboard chi
     * tinh cho <b>tu MOI</b> ({@code new_items_learned}), ma {@code awardXp} khong
     * biet luot tra nay co phai tu moi hay khong. Caller ({@code StudyTrackerService})
     * tu goi {@link #syncLeaderboardAfterCommit(Long)} khi {@code isNew == 1}.</p>
     */
    private int leaderboardPoints(XpSource source) {
        if (source == null) return 0;
        return switch (source) {
            case LESSON_COMPLETE -> getLessonPoints();
            default              -> 0;
        };
    }

    /**
     * Dong bo diem leaderboard cua user trong tuan hien tai vao Redis, chay
     * <b>sau khi transaction commit</b>.
     *
     * <p>Dung cho luong tra tu: chi goi khi luot tra la <b>tu MOI</b>
     * ({@code new_items_learned} vua tang) — vi diem xep hang tinh theo tu moi,
     * khong tinh theo tong lan tra.</p>
     */
    public void syncLeaderboardAfterCommit(Long userId) {
        if (userId == null) {
            return;
        }
        final String weekKey = LeaderboardWeekUtil.currentWeekKey(clock);
        registerAfterCommit(() -> leaderboardService.syncUserScore(userId, weekKey));
    }

    /**
     * Lay learned_item_points tu app_config (diem moi tu MOI da hoc).
     */
    public int getLearnedItemPoints() {
        return appConfigService.getIntValue(CFG_LEARNED_ITEM_POINTS, DEFAULT_LEARNED_ITEM_POINTS);
    }

    /**
     * Lay lesson_points tu app_config.
     */
    public int getLessonPoints() {
        return appConfigService.getIntValue(CFG_LESSON_POINTS, DEFAULT_LESSON_POINTS);
    }

    /**
     * Lay lookup_xp tu app_config.
     */
    public int getLookupXp() {
        return appConfigService.getIntValue(CFG_LOOKUP_XP, DEFAULT_XP_PER_LOOKUP);
    }

    /**
     * Lay lesson_xp tu app_config.
     */
    public int getLessonXp() {
        return appConfigService.getIntValue(CFG_LESSON_XP, DEFAULT_XP_PER_LESSON);
    }

    // -------------------------------------------------------------------------
    // Streak
    // -------------------------------------------------------------------------

    /**
     * Cap nhat streak khi user co hoat dong.
     *
     * <p>Quy tac:</p>
     * <ul>
     *   <li>Chua co {@code lastActivityDate} -> streak = 1</li>
     *   <li>Ban ghi ngay hom qua -> streak += 1</li>
     *   <li>Ban ghi ngay hom nay -> giu nguyen streak</li>
     *   <li>Gap > 1 ngay -> streak reset = 1</li>
     * </ul>
     */
    @Transactional
    public void touchActivity(Long userId) {
        UserGamification gamification = getOrCreate(userId);
        streakService.updateStreak(gamification);
        gamificationRepository.save(gamification);
    }

    // -------------------------------------------------------------------------
    // AfterCommit helpers
    // -------------------------------------------------------------------------

    /**
     * Dang ky action de chay sau khi transaction commit thanh cong.
     */
    private void registerAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                }
            );
        } else {
            // Khong co transaction, chay ngay
            action.run();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public UserGamification getOrNull(Long userId) {
        return gamificationRepository.findById(userId).orElse(null);
    }

    private UserGamification getOrCreate(Long userId) {
        return gamificationRepository.findById(userId).orElseGet(() -> {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                throw new IllegalArgumentException("User not found: " + userId);
            }
            return UserGamification.builder()
                    .userId(userId)
                    .user(user)
                    .totalXp(0L)
                    .currentStreak(0)
                    .longestStreak(0)
                    .build();
        });
    }
}
