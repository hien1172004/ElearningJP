package com.jp.elearningjp.service.notification;

import com.jp.elearningjp.repository.gamification.NotificationRepository;
import com.jp.elearningjp.shared.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import net.javacrumbs.shedlock.core.LockProvider;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test cho {@link NotificationScheduler} — chống trùng thông báo nhắc nhở.
 *
 * <p>Chạy trên PostgreSQL thật (Testcontainers) vì lớp chống trùng thứ 2 là
 * <b>unique index</b> {@code uq_notifications_daily_reminder}
 * {@code (user_id, type, ngày Asia/Ho_Chi_Minh)} — H2/HSQLDB không hỗ trợ index
 * expression nên test sẽ cho kết quả sai.</p>
 *
 * <p><b>Không dùng {@code @Transactional}:</b> {@code NotificationService.create}
 * chạy {@code REQUIRES_NEW} → transaction con không thấy row user mới insert trong
 * transaction test (chưa commit) → vi phạm FK. Mỗi test tự tạo user riêng
 * (email unique).</p>
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.hibernate.show-sql=false",
        "spring.data.redis.repositories.enabled=false",
})
class NotificationSchedulerIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("elearning_test")
            .withUsername("postgres")
            .withPassword("postgres");

    /**
     * Vô hiệu hoá ShedLock trong test.
     *
     * <p><b>Vì sao bắt buộc?</b> Job khai báo {@code lockAtLeastFor = "PT1M"}: khoá
     * được giữ <i>tối thiểu</i> 1 phút sau khi job xong. Test gọi job nhiều lần trong
     * vài giây nên từ lần thứ 2 trở đi <b>không giành được khoá</b> và thân method
     * không chạy. Hệ quả: các test khẳng định "có tạo thông báo" fail, còn các test
     * khẳng định "không tạo" lại pass một cách giả tạo.</p>
     *
     * <p>Test này chỉ kiểm chứng <b>lớp chống trùng nghiệp vụ</b>
     * ({@code existsByUser_IdAndTypeAndCreatedAtBetween} + unique index theo ngày);
     * hành vi khoá phân tán là việc của ShedLock, không phải của job. Đặt tên bean
     * trùng {@code lockProvider} + bật override để thay thế bean thật.</p>
     */
    @TestConfiguration
    static class NoopLockConfig {
        @Bean
        @Primary
        LockProvider noopLockProvider() {
            return configuration -> Optional.of(() -> {
            });
        }
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired NotificationScheduler scheduler;
    @Autowired NotificationRepository notificationRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private Long userId;

    @BeforeEach
    void setUp() {
        String email = "notif-" + System.nanoTime() + "@test.local";
        jdbcTemplate.update(
                "INSERT INTO users (email, password_hash, full_name, auth_provider, status, deleted, version) "
                        + "VALUES (?, ?, ?, 'LOCAL', 'ACTIVE', FALSE, 0)",
                email, "hash", "Notif User");
        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    // ================================================================
    // SRS_DUE
    // ================================================================

    @Test
    @DisplayName("SRS_DUE: không có thẻ đến hạn → không tạo thông báo")
    void srsDue_noDueCards_noNotification() {
        scheduler.sendSrsDueReminders();
        assertThat(countOf(NotificationType.SRS_DUE)).isZero();
    }

    @Test
    @DisplayName("SRS_DUE: có thẻ đến hạn → tạo đúng 1 thông báo")
    void srsDue_withDueCard_createsOneNotification() {
        insertDueSrsItem(userId);

        scheduler.sendSrsDueReminders();

        assertThat(countOf(NotificationType.SRS_DUE)).isEqualTo(1);
    }

    @Test
    @DisplayName("SRS_DUE: chạy job 2 lần trong ngày → chỉ 1 thông báo (chống trùng lớp 1)")
    void srsDue_runTwice_sameDay_onlyOneNotification() {
        insertDueSrsItem(userId);

        scheduler.sendSrsDueReminders();
        scheduler.sendSrsDueReminders();

        assertThat(countOf(NotificationType.SRS_DUE)).isEqualTo(1);
    }

    @Test
    @DisplayName("SRS_DUE: notify_srs_due = false → không tạo thông báo")
    void srsDue_userOptedOut_noNotification() {
        insertDueSrsItem(userId);
        disableSetting("notify_srs_due");

        scheduler.sendSrsDueReminders();

        assertThat(countOf(NotificationType.SRS_DUE)).isZero();
    }

    // ================================================================
    // STREAK_WARNING
    // ================================================================

    @Test
    @DisplayName("STREAK_WARNING: user mới chưa có streak → không cảnh báo")
    void streakWarning_noStreak_noNotification() {
        scheduler.sendStreakWarnings();
        assertThat(countOf(NotificationType.STREAK_WARNING)).isZero();
    }

    @Test
    @DisplayName("STREAK_WARNING: chuỗi 3 ngày, hôm nay chưa học → cảnh báo")
    void streakWarning_activeStreak_notStudiedToday_warns() {
        seedStreak(userId, 3);

        scheduler.sendStreakWarnings();

        assertThat(countOf(NotificationType.STREAK_WARNING)).isEqualTo(1);
    }

    @Test
    @DisplayName("STREAK_WARNING: hôm nay đã học mục mới → KHÔNG cảnh báo")
    void streakWarning_studiedToday_noWarning() {
        seedStreak(userId, 3);
        insertDailyStats(userId, LocalDate.now(), 1);

        scheduler.sendStreakWarnings();

        assertThat(countOf(NotificationType.STREAK_WARNING)).isZero();
    }

    @Test
    @DisplayName("STREAK_WARNING: chuỗi đã đứt (hôm qua cũng không học) → KHÔNG cảnh báo")
    void streakWarning_brokenStreak_noWarning() {
        // Chỉ hoạt động 5 ngày trước → chuỗi đã đứt từ lâu
        insertDailyStats(userId, LocalDate.now().minusDays(5), 3);
        insertGamification(userId, 5);

        scheduler.sendStreakWarnings();

        assertThat(countOf(NotificationType.STREAK_WARNING)).isZero();
    }

    @Test
    @DisplayName("STREAK_WARNING: notify_streak_warning = false → không cảnh báo")
    void streakWarning_userOptedOut_noWarning() {
        seedStreak(userId, 3);
        disableSetting("notify_streak_warning");

        scheduler.sendStreakWarnings();

        assertThat(countOf(NotificationType.STREAK_WARNING)).isZero();
    }

    @Test
    @DisplayName("STREAK_WARNING: chạy job 2 lần trong ngày → chỉ 1 thông báo")
    void streakWarning_runTwice_sameDay_onlyOneNotification() {
        seedStreak(userId, 3);

        scheduler.sendStreakWarnings();
        scheduler.sendStreakWarnings();

        assertThat(countOf(NotificationType.STREAK_WARNING)).isEqualTo(1);
    }

    // ================================================================
    // Lớp chống trùng thứ 2 — unique index theo ngày
    // ================================================================

    @Test
    @DisplayName("Unique index chặn 2 thông báo nhắc nhở cùng user + cùng loại + cùng ngày")
    void uniqueIndex_blocksSecondReminderSameDay() {
        insertDueSrsItem(userId);

        // Bỏ qua lớp chống trùng lớp 1 (application) — insert thẳng để chứng minh
        // ràng buộc ở tầng DB thực sự chặn bản ghi thứ hai.
        jdbcTemplate.update(
                "INSERT INTO notifications (user_id, title, body, type, is_read, deleted, created_at, updated_at, version) "
                        + "VALUES (?, 't', 'b', 'SRS_DUE', FALSE, FALSE, now(), now(), 0)",
                userId);

        // Lần chạy job tiếp theo sẽ đụng unique index → bị bỏ qua, không tạo bản ghi thứ 3.
        scheduler.sendSrsDueReminders();

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'SRS_DUE'",
                Integer.class, userId);
        assertThat(rows).isEqualTo(1);
    }

    // ================================================================
    // Helpers
    // ================================================================

    private long countOf(NotificationType type) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = ?",
                Long.class, userId, type.name());
    }

    /** 1 thẻ SRS đến hạn (next_review_at <= now). */
    private void insertDueSrsItem(Long uid) {
        jdbcTemplate.update(
                "INSERT INTO srs_items (user_id, card_type, repetition_count, lapse_count, ease_factor, "
                        + "interval_days, next_review_at, status, deleted, created_at, updated_at, version) "
                        + "VALUES (?, 'STANDARD', 0, 0, 2.50, "
                        + "1, now() - interval '1 day', 'LEARNING', FALSE, now(), now(), 0)",
                uid);
    }

    /** Chuỗi N ngày liên tiếp KẾT THÚC Ở HÔM QUA (hôm nay chưa học). */
    private void seedStreak(Long uid, int days) {
        for (int i = 1; i <= days; i++) {
            insertDailyStats(uid, LocalDate.now().minusDays(i), 2);
        }
        insertGamification(uid, days);
    }

    private void insertDailyStats(Long uid, LocalDate date, int newItems) {
        jdbcTemplate.update(
                "INSERT INTO user_daily_stats (user_id, stat_date, lookups, lookups_scored, xp_earned, "
                        + "goal_words, goal_met, lessons_completed, new_items_learned) "
                        + "VALUES (?, ?, 0, 0, 0, 5, TRUE, 0, ?)",
                uid, date, newItems);
    }

    private void insertGamification(Long uid, int streak) {
        jdbcTemplate.update(
                "INSERT INTO user_gamification (user_id, total_xp, current_streak, longest_streak, created_at, updated_at) "
                        + "VALUES (?, 0, ?, ?, now(), now())",
                uid, streak, streak);
    }

    private void disableSetting(String column) {
        jdbcTemplate.update(
                "INSERT INTO user_settings (user_id, reminder_time, push_enabled, notify_srs_due, "
                        + "notify_streak_warning, notify_task_reminder, notify_exam_result, notify_community, "
                        + "created_at, updated_at, version) "
                        + "VALUES (?, '20:00:00', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, now(), now(), 0)",
                userId);
        // column là tên cột boolean đã được whitelist ở caller.
        jdbcTemplate.update(
                "UPDATE user_settings SET " + column + " = FALSE WHERE user_id = ?", userId);
    }
}
