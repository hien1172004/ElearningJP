package com.jp.elearningjp.service.notification;

import com.jp.elearningjp.config.TimeConfig;
import com.jp.elearningjp.repository.gamification.NotificationRepository;
import com.jp.elearningjp.repository.gamification.UserDailyStatsRepository;
import com.jp.elearningjp.repository.gamification.UserGamificationRepository;
import com.jp.elearningjp.repository.srs.SrsItemRepository;
import com.jp.elearningjp.repository.user.UserSettingsRepository;
import com.jp.elearningjp.service.gamification.StreakService;
import com.jp.elearningjp.shared.enums.NotificationType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Job nhắc nhở hằng ngày — nguồn {@code SRS_DUE} và {@code STREAK_WARNING}.
 *
 * <p><b>Chống chạy trùng khi scale ngang (2 lớp):</b></p>
 * <ol>
 *   <li>{@code @SchedulerLock} — chỉ 1 instance giành được khoá trong bảng {@code shedlock}.</li>
 *   <li>Unique index {@code uq_notifications_daily_reminder} — nếu 2 instance vẫn lọt qua
 *       (vd instance trước chết giữa lúc chạy), bản ghi thứ hai vi phạm ràng buộc
 *       {@code (user_id, type, ngày VN)} và bị bỏ qua.</li>
 * </ol>
 *
 * <p><b>Mỗi user xử lý trong try/catch riêng:</b> một user lỗi (dữ liệu bẩn, vi phạm
 * ràng buộc) không được làm dừng cả job của hàng nghìn user khác.</p>
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationScheduler {

    /** Cửa sổ quét ngược tối đa khi tính streak (chặn query quá rộng). */
    private static final int STREAK_LOOKBACK_DAYS = 400;

    NotificationService notificationService;
    NotificationRepository notificationRepository;
    UserSettingsRepository userSettingsRepository;
    SrsItemRepository srsItemRepository;
    UserGamificationRepository userGamificationRepository;
    UserDailyStatsRepository userDailyStatsRepository;
    StreakService streakService;
    Clock clock;

    // ============================================================
    // SRS_DUE — 08:00 giờ VN
    // ============================================================

    @Scheduled(cron = "0 0 8 * * *", zone = TimeConfig.APP_ZONE)
    @SchedulerLock(name = "srsDueJob", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void sendSrsDueReminders() {
        Instant now = Instant.now(clock);
        LocalDate today = LocalDate.now(clock);

        List<Object[]> rows = srsItemRepository.countDueCardsGroupByUser(now);
        if (rows.isEmpty()) {
            log.debug("[Notification] SRS job: no due cards");
            return;
        }

        Set<Long> optedOut = new HashSet<>(userSettingsRepository.findUserIdsWithSrsDueDisabled());

        int sent = 0;
        int skipped = 0;
        for (Object[] row : rows) {
            Long userId = ((Number) row[0]).longValue();
            long dueCount = ((Number) row[1]).longValue();

            if (dueCount <= 0 || optedOut.contains(userId)) {
                skipped++;
                continue;
            }

            if (alreadySentToday(userId, NotificationType.SRS_DUE, today)) {
                skipped++;
                continue;
            }

            try {
                notificationService.create(
                        userId,
                        NotificationType.SRS_DUE,
                        "Bạn có thẻ SRS cần ôn tập",
                        "Hôm nay bạn có " + dueCount + " thẻ đến hạn ôn tập. Ôn ngay để không quên nhé!",
                        null
                );
                sent++;
            } catch (DataIntegrityViolationException ex) {
                // Instance khác đã tạo trước (unique index theo ngày) — bỏ qua.
                skipped++;
            } catch (Exception ex) {
                log.warn("[Notification] SRS reminder failed for user {}: {}", userId, ex.getMessage());
                skipped++;
            }
        }

        log.info("[Notification] SRS job done: sent={}, skipped={}", sent, skipped);
    }

    // ============================================================
    // STREAK_WARNING — 20:30 giờ VN
    // ============================================================

    @Scheduled(cron = "0 30 20 * * *", zone = TimeConfig.APP_ZONE)
    @SchedulerLock(name = "streakWarningJob", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void sendStreakWarnings() {
        LocalDate today = LocalDate.now(clock);

        // Pre-filter rẻ: chỉ user từng có streak mới cần cảnh báo.
        List<Long> candidates = userGamificationRepository.findUserIdsWithPositiveStreak();
        if (candidates.isEmpty()) {
            log.debug("[Notification] Streak job: no candidates");
            return;
        }

        Set<Long> optedOut = new HashSet<>(userSettingsRepository.findUserIdsWithStreakWarningDisabled());

        int sent = 0;
        int skipped = 0;
        for (Long userId : candidates) {
            if (optedOut.contains(userId)) {
                skipped++;
                continue;
            }

            try {
                if (!shouldWarnStreak(userId, today)) {
                    skipped++;
                    continue;
                }

                if (alreadySentToday(userId, NotificationType.STREAK_WARNING, today)) {
                    skipped++;
                    continue;
                }

                int streak = currentStreak(userId, today);
                notificationService.create(
                        userId,
                        NotificationType.STREAK_WARNING,
                        "Chuỗi học của bạn sắp đứt!",
                        "Bạn đang có chuỗi " + streak + " ngày. Học ít nhất 1 mục mới hôm nay để giữ chuỗi nhé!",
                        null
                );
                sent++;
            } catch (DataIntegrityViolationException ex) {
                skipped++;
            } catch (Exception ex) {
                log.warn("[Notification] streak warning failed for user {}: {}", userId, ex.getMessage());
                skipped++;
            }
        }

        log.info("[Notification] Streak job done: sent={}, skipped={}", sent, skipped);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    /**
     * Điều kiện cảnh báo: hôm nay CHƯA học mục mới, nhưng chuỗi vẫn còn sống
     * (streak >= 1). Nếu hôm qua cũng không học thì chuỗi đã đứt — cảnh báo vô nghĩa.
     *
     * <p>Dùng đúng nguồn sự thật {@code user_daily_stats.new_items_learned > 0} theo
     * chính sách STRICT, không đọc cache {@code user_gamification.current_streak}.</p>
     */
    private boolean shouldWarnStreak(Long userId, LocalDate today) {
        List<LocalDate> activeDates = userDailyStatsRepository.findStreakWindow(
                userId, today, today.minusDays(STREAK_LOOKBACK_DAYS));

        if (activeDates.isEmpty()) {
            return false;
        }
        // Đã học hôm nay → chuỗi an toàn, không cần nhắc.
        if (activeDates.contains(today)) {
            return false;
        }
        // Chuỗi phải còn "sống" (kết thúc ở hôm qua) thì mới sắp đứt.
        return streakService.computeCurrentStreakFromDesc(activeDates, today) >= 1;
    }

    private int currentStreak(Long userId, LocalDate today) {
        List<LocalDate> activeDates = userDailyStatsRepository.findStreakWindow(
                userId, today, today.minusDays(STREAK_LOOKBACK_DAYS));
        return streakService.computeCurrentStreakFromDesc(activeDates, today);
    }

    /**
     * Lớp chống trùng thứ 1 (application): đã có thông báo cùng loại trong ngày VN chưa.
     * Khoảng là {@code [00:00 hôm nay, 00:00 mai)} theo giờ VN.
     */
    private boolean alreadySentToday(Long userId, NotificationType type, LocalDate today) {
        ZoneId zone = ZoneId.of(TimeConfig.APP_ZONE);
        Instant from = today.atStartOfDay(zone).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(zone).toInstant().truncatedTo(ChronoUnit.SECONDS);
        return notificationRepository.existsByUser_IdAndTypeAndCreatedAtBetween(
                userId, type, from, to);
    }
}
