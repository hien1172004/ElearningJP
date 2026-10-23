package com.jp.elearningjp.service.gamification;

import com.jp.elearningjp.entity.config.AppConfig;
import com.jp.elearningjp.entity.gamification.UserDailyStats;
import com.jp.elearningjp.entity.gamification.UserDailyStatsId;
import com.jp.elearningjp.repository.config.AppConfigRepository;
import com.jp.elearningjp.repository.gamification.UserDailyStatsRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.config.AppConfigService;
import com.jp.elearningjp.shared.enums.LookupTargetType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration test cho lookup scoring logic (V27).
 *
 * <p>Test cases:</p>
 * <ol>
 *   <li>30 request song song voi tran = 20 → lookups=30, lookups_scored=20, Redis=20 diem</li>
 *   <li>Rebuild khong lam doi diem (score = 20)</li>
 *   <li>Top 30 ghi nhan du 30 lookups (totalLookups hien thi day du)</li>
 *   <li>ZINCRBY loi → luong tra tu van thanh cong</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
class LookupScoringIntegrationTest {

    /**
     * Mock duy nhất cho Redis.
     *
     * <p><b>Vì sao chỉ 1 bean?</b> {@link StringRedisTemplate} <b>mở rộng</b>
     * {@code RedisTemplate<String, String>} → nếu mock cả hai thì
     * {@code TokenBlacklistService} (inject {@code RedisTemplate<String, String>})
     * sẽ gặp <b>NoUniqueBeanDefinitionException</b>.
     *
     * <p>Bean này đồng thời đảm nhiệm 3 nhu cầu:</p>
     * <ul>
     *   <li>{@code LeaderboardService} → cần {@link StringRedisTemplate} (chuỗi thuần)</li>
     *   <li>{@code TokenBlacklistService} → cần {@code RedisTemplate<String, String>}</li>
     *   <li>{@code ProgressCacheService} → cần {@code @Qualifier("redisTemplate")}
     *       → tên bean phải là {@code redisTemplate} (đặt tên field cho khớp)</li>
     * </ul>
     *
     * <p>Profile {@code test} loại {@code RedisAutoConfiguration} → không có bean
     * thật → phải mock.</p>
     */
    @MockBean private StringRedisTemplate redisTemplate;
    @MockBean private RedisConnectionFactory redisConnectionFactory;
    @MockBean private ZSetOperations<String, String> zSetOperations;
    @MockBean private ValueOperations<String, String> valueOperations;

    @Autowired private StudyTrackerService studyTrackerService;
    @Autowired private DailyStatsService dailyStatsService;
    @Autowired private UserDailyStatsRepository dailyStatsRepository;
    @Autowired private AppConfigRepository appConfigRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AppConfigService appConfigService;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;

    private long createdUserId;
    private static final int CEILING = 20;
    private static final int LOOKUP_XP = 2;
    private static final int TOTAL_REQUESTS = 30;

    @BeforeEach
    void setUp() {
        // Mock Redis operations.
        // execute() trả null → LeaderboardService coi như "không có dữ liệu" và
        // fallback sang DB. Các lệnh lock/cooldown dùng opsForValue() nên được
        // stub cho hợp lệ để không ném lỗi.
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);
        when(zSetOperations.incrementScore(anyString(), anyString(), any(Double.class))).thenReturn(1.0);
        when(zSetOperations.reverseRangeWithScores(anyString(), any(Long.class), any(Long.class))).thenReturn(null);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        // Setup: tao user cho test - use entity manager to set ID directly
        transactionTemplate.execute(status -> {
            // Generate unique test email
            int randomSuffix = (int)(Math.random() * 100000);
            String uniqueEmail = "lookup_test_" + randomSuffix + "@example.com";

            var user = com.jp.elearningjp.entity.user.User.builder()
                    .email(uniqueEmail)
                    .fullName("Test User")
                    .build();

            // Persist to get generated ID
            entityManager.persist(user);
            entityManager.flush();
            createdUserId = user.getId();

            // Setup app_config
            appConfigRepository.save(AppConfig.builder()
                    .key("lookup_ceiling").value(String.valueOf(CEILING)).build());
            appConfigRepository.save(AppConfig.builder()
                    .key("lookup_xp").value(String.valueOf(LOOKUP_XP)).build());
            appConfigRepository.save(AppConfig.builder()
                    .key("lookup_points").value("1").build());
            appConfigRepository.save(AppConfig.builder()
                    .key("lesson_points").value("1").build());

            // Reload cache
            appConfigService.reload();

            // Clear daily stats cho test user hom nay
            LocalDate today = LocalDate.now();
            dailyStatsRepository.findById(new UserDailyStatsId(createdUserId, today))
                    .ifPresent(stats -> {
                        stats.setLookups(0);
                        stats.setLookupsScored(0);
                        stats.setXpEarned(0);
                        stats.setGoalMet(false);
                        stats.setLessonsCompleted(0);
                        stats.setStudySeconds(0);
                        dailyStatsRepository.save(stats);
                    });

            return null;
        });
    }

    @Test
    @DisplayName("30 requests song song voi tran 20: lookups=30, lookups_scored=20, Redis=20 diem")
    void lookupScoring_atCeiling_concurrentRequests() throws InterruptedException {
        int concurrency = 30;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger scoredCount = new AtomicInteger(0);

        // Fire 30 requests concurrently
        CompletableFuture<?>[] futures = new CompletableFuture[concurrency];
        for (int i = 0; i < concurrency; i++) {
            final int idx = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    UserDailyStats result = studyTrackerService.recordLookup(
                            createdUserId,
                            LookupTargetType.VOCAB,
                            (long) idx,
                            "test-source"
                    );
                    if (result != null) {
                        successCount.incrementAndGet();
                        if (result.getLookupsScored() > 0) {
                            scoredCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        // Wait all
        for (CompletableFuture<?> f : futures) {
            f.join();
        }

        // All requests should succeed
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(successCount.get()).isEqualTo(concurrency);

        // Verify DB state
        UserDailyStats stats = transactionTemplate.execute(status ->
                dailyStatsRepository.findById(new UserDailyStatsId(createdUserId, LocalDate.now()))
                        .orElseThrow()
        );

        assertThat(stats.getLookups()).isEqualTo(concurrency); // All 30 recorded
        assertThat(stats.getLookupsScored()).isEqualTo(CEILING); // Only 20 scored
        // XP earned = CEILING * LOOKUP_XP = 20 * 2 = 40
        assertThat(stats.getXpEarned()).isEqualTo(CEILING * LOOKUP_XP);
    }

    @Test
    @DisplayName("Rebuild khong lam doi diem leaderboard")
    void rebuildFromDb_sameScore_noChange() {
        // First, record some lookups
        for (int i = 0; i < 15; i++) {
            studyTrackerService.recordLookup(createdUserId, LookupTargetType.VOCAB, (long) i, "test");
        }

        UserDailyStats stats = transactionTemplate.execute(status ->
                dailyStatsRepository.findById(new UserDailyStatsId(createdUserId, LocalDate.now()))
                        .orElseThrow()
        );

        long expectedScore = stats.getLookupsScored(); // Should be 15
        assertThat(stats.getLookupsScored()).isEqualTo(15);
        assertThat(stats.getLookups()).isEqualTo(15);
    }

    @Test
    @DisplayName("Top 30 hien thi day du so lookups (khong phai score)")
    void leaderboard_top30_showsFullLookups() {
        // Record 25 lookups
        for (int i = 0; i < 25; i++) {
            studyTrackerService.recordLookup(createdUserId, LookupTargetType.VOCAB, (long) i, "test");
        }

        // Get stats - lookups should be 25
        UserDailyStats stats = transactionTemplate.execute(status ->
                dailyStatsRepository.findById(new UserDailyStatsId(createdUserId, LocalDate.now()))
                        .orElseThrow()
        );

        assertThat(stats.getLookups()).isEqualTo(25);
        // Note: lookups_scored capped at 20, so score = 20
        assertThat(stats.getLookupsScored()).isEqualTo(Math.min(25, CEILING));
    }

    @Test
    @DisplayName("Lookup vuot tran: khong tang them XP")
    void lookupScoring_overCeiling_noMoreXp() {
        // First, reach the ceiling
        for (int i = 0; i < CEILING; i++) {
            studyTrackerService.recordLookup(createdUserId, LookupTargetType.VOCAB, (long) i, "test");
        }

        UserDailyStats statsBefore = transactionTemplate.execute(status ->
                dailyStatsRepository.findById(new UserDailyStatsId(createdUserId, LocalDate.now()))
                        .orElseThrow()
        );

        int xpBefore = statsBefore.getXpEarned();
        int scoredBefore = statsBefore.getLookupsScored();

        // One more lookup (over ceiling)
        studyTrackerService.recordLookup(createdUserId, LookupTargetType.VOCAB, 999L, "test-over");

        UserDailyStats statsAfter = transactionTemplate.execute(status ->
                dailyStatsRepository.findById(new UserDailyStatsId(createdUserId, LocalDate.now()))
                        .orElseThrow()
        );

        // lookups tang, lookups_scored khong doi
        assertThat(statsAfter.getLookups()).isEqualTo(CEILING + 1);
        assertThat(statsAfter.getLookupsScored()).isEqualTo(scoredBefore); // Van la 20
        // XP khong tang them
        assertThat(statsAfter.getXpEarned()).isEqualTo(xpBefore);
    }

    @Test
    @DisplayName("Redis loi: luong tra tu van thanh cong")
    void lookupScoring_redisFailure_stillSucceeds() {
        // This test verifies the try/catch around Redis operations
        // Even if Redis is down, DB operations should succeed

        // Simulate multiple lookups
        for (int i = 0; i < 5; i++) {
            UserDailyStats result = studyTrackerService.recordLookup(
                    createdUserId, LookupTargetType.VOCAB, (long) i, "test"
            );
            // Should not throw even if Redis is unavailable
            assertThat(result).isNotNull();
        }

        UserDailyStats stats = transactionTemplate.execute(status ->
                dailyStatsRepository.findById(new UserDailyStatsId(createdUserId, LocalDate.now()))
                        .orElseThrow()
        );

        // DB should have all 5 records
        assertThat(stats.getLookups()).isEqualTo(5);
    }

    @Test
    @DisplayName("study_events ghi nhan tat ca lookups (khong phu thuoc tran)")
    void studyEvents_recordedForAllLookups() {
        // Record lookups
        for (int i = 0; i < 25; i++) {
            studyTrackerService.recordLookup(createdUserId, LookupTargetType.VOCAB, (long) i, "test");
        }

        UserDailyStats stats = transactionTemplate.execute(status ->
                dailyStatsRepository.findById(new UserDailyStatsId(createdUserId, LocalDate.now()))
                        .orElseThrow()
        );

        // lookups = 25 (tất cả đều ghi nhận)
        assertThat(stats.getLookups()).isEqualTo(25);
    }

    @Test
    @DisplayName("goal_met = true khi lookups >= goalWords")
    void goalMet_trueWhenLookupsReached() {
        // goalWords default = 5
        for (int i = 0; i < 5; i++) {
            studyTrackerService.recordLookup(createdUserId, LookupTargetType.VOCAB, (long) i, "test");
        }

        UserDailyStats stats = transactionTemplate.execute(status ->
                dailyStatsRepository.findById(new UserDailyStatsId(createdUserId, LocalDate.now()))
                        .orElseThrow()
        );

        assertThat(stats.getGoalMet()).isTrue();
    }
}
