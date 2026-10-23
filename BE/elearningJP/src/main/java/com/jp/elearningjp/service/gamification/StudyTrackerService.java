package com.jp.elearningjp.service.gamification;

import com.jp.elearningjp.entity.gamification.UserDailyStats;
import com.jp.elearningjp.service.progress.ProgressCacheService;
import com.jp.elearningjp.shared.enums.LookupTargetType;
import com.jp.elearningjp.shared.enums.XpSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Diem vao duy nhat cho moi hoat dong ghi su kien hoc tap.
 *
 * <p>Moi lan goi se cap nhat dong thoi 3 noi:</p>
 * <ol>
 *   <li>{@code user_daily_stats} — so tu da hoc, bai hoan thanh, XP trong ngay, goal_met</li>
 *   <li>{@code study_events} — append-only de tinh top-lookups</li>
 *   <li>{@code user_gamification} + {@code xp_logs} — tong XP + streak</li>
 * </ol>
 *
 * <p><b>Lookup scoring:</b></p>
 * <ul>
 *   <li>lookups: tang moi lan tra (khong tran)</li>
 *   <li>lookups_scored: chi tang khi < ceiling (dung cho leaderboard)</li>
 *   <li>Redis ZINCRBY: chi goi sau commit khi scored=true</li>
 * </ul>
 *
 * <p>Thiet ke theo nguyen tac "analytics khong duoc lam fail UX":
 * moi nhanh deu boc try/catch rieng, loi chi log warn.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyTrackerService {

    private final DailyStatsService dailyStatsService;
    private final StudyEventService studyEventService;
    private final XpService xpService;
    private final ProgressCacheService progressCacheService;

    /**
     * Ghi nhận 1 lần tra từ điển.
     *
     * <p>Luong xu ly:</p>
     * <ol>
     *   <li>Ghi study_events (lookup) - moi lan tra deu ghi</li>
     *   <li>Upsert daily_stats (lookups tang, lookups_scored chi tang khi < ceiling)</li>
     *   <li>Neu scored=true: cong XP + streak + ZINCRBY (sau commit)</li>
     *   <li>Neu scored=false: da vuot tran, khong cong XP</li>
     * </ol>
     *
     * @return daily stats hom nay sau khi cap nhat (co the null neu mọi nhánh đều fail)
     */
    public UserDailyStats recordLookup(Long userId, LookupTargetType targetType, Long targetId, String sourceRef) {
        // 1. Ghi study_events - moi lan tra deu ghi (khong phu thuoc tran)
        try {
            studyEventService.logLookup(userId, targetType, targetId, sourceRef, 0);
        } catch (Exception ex) {
            log.warn("[StudyTracker] studyEvents.logLookup failed for user {}: {}", userId, ex.getMessage());
        }

        // 2. Daily stats - upsert nguyen tu, tra ve scored flag
        DailyStatsService.LookupResult result;
        try {
            result = dailyStatsService.recordLookup(userId, xpService.getLookupXp());
        } catch (Exception ex) {
            log.warn("[StudyTracker] dailyStats.recordLookup failed for user {}: {}", userId, ex.getMessage());
            return null;
        }

        // 3. Chi cong XP + ZINCRBY khi duoc tinh diem (chua vuot tran)
        if (result != null && result.scored()) {
            try {
                xpService.awardXp(userId, result.stats().getXpEarned(), XpSource.DAILY_TASK, targetId);
                xpService.touchActivity(userId);
            } catch (Exception ex) {
                log.warn("[StudyTracker] awardXp failed for user {}: {}", userId, ex.getMessage());
            }
        }

        // 4. Vo hieu hoa cache Progress (generation bump) sau khi commit
        invalidateProgressCache(userId);

        return result != null ? result.stats() : null;
    }

    /**
     * Ghi nhận hoàn thành bài học.
     *
     * @param lessonId bài học vừa hoàn thành (dùng làm sourceRefId, chống ghi trùng)
     * @param xpReward XP thưởng cho bài học
     */
    public void recordLessonComplete(Long userId, Long lessonId, Long courseId, int xpReward) {
        int xp = Math.max(0, xpReward);

        // Daily stats: tang lessons_completed + cong XP (chi lan dau tien moi bai)
        //
        // <b>THU TU QUAN TRONG</b>: phai chay TRUOC awardXp.
        // awardXp -> afterCommit -> syncUserScore doc diem tuyet doi trong tuan tu DB.
        // Neu awardXp chay truoc, dong lessons_completed chua duoc ghi -> sync doc
        // thieu diem bai hoc -> ZSET lech DB. Sau khi dao thu tu: daily stats da
        // COMMIT (REQUIRES_NEW) -> awardXp -> afterCommit doc DB thay du.
        safeStep("lessonDailyStats", () ->
                dailyStatsService.recordLessonComplete(userId, xp));

        safeStep("lessonXp", () -> {
            if (xp > 0) {
                xpService.awardXp(userId, xp, XpSource.LESSON_COMPLETE, lessonId);
            }
            xpService.touchActivity(userId);
        });

        safeStep("logLessonComplete", () -> studyEventService.logLessonComplete(userId, lessonId, xp));
    }

    private void safeStep(String name, Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            log.warn("[StudyTracker] step '{}' failed: {}", name, ex.getMessage());
        }
    }

    /**
     * Vo hieu hoa cache Progress cua user bang cach bump generation token.
     *
     * <p><b>Vi sao sau commit?</b> Neu bump truoc khi commit, request song song
     * co the doc lai du lieu cu (chua commit) va cache lai -> van thay 0.
     * Chi bump <b>sau khi transaction commit thanh cong</b> moi dam bao
     * lan doc tiep theo thay du lieu moi.</p>
     *
     * <p>Redis loi -> chi log warn, khong anh huong luong ghi chinh.</p>
     */
    private void invalidateProgressCache(Long userId) {
        if (userId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            doInvalidate(userId);
                        }
                    });
        } else {
            doInvalidate(userId);
        }
    }

    private void doInvalidate(Long userId) {
        try {
            progressCacheService.invalidate(userId);
        } catch (Exception ex) {
            log.warn("[StudyTracker] invalidate progress cache fail for user {}: {}",
                    userId, ex.getMessage());
        }
    }
}
