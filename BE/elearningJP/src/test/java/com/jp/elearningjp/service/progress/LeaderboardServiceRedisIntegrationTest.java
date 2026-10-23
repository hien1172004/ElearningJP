package com.jp.elearningjp.service.progress;

import com.jp.elearningjp.service.progress.LeaderboardService.LeaderboardRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test cho {@link LeaderboardService} — chạy với <b>Redis THẬT</b>.
 *
 * <p><b>Vì sao bắt buộc phải dùng Redis thật?</b></p>
 * <ul>
 *   <li>{@code ZADD GT} — hành vi <b>phía server</b> (Redis tự so sánh score với
 *       score đang có rồi mới quyết định ghi). Mock {@code ZSetOperations}
 *       <b>không</b> mô phỏng được: nó luôn "ghi thành công".</li>
 *   <li>{@code ZCOUNT (score, +inf)} — cú pháp exclusive {@code "(score"} là
 *       <b>chi tiết encode phía driver</b> (Lettuce). Nếu driver encode thành
 *       inclusive thì hạng của user đồng điểm bị lệch (2 thay vì 1).</li>
 * </ul>
 *
 * <p>Hai test "GT out-of-order" và "rebuild không đè bản mới" chỉ có ý nghĩa khi
 * chạy trên Redis thật. Nếu chỉ mock, test sẽ "xanh" nhưng hành vi thật chưa
 * được kiểm chứng.</p>
 *
 * <p><b>Yêu cầu phiên bản:</b> Redis >= 6.2 (cú pháp exclusive range chỉ có từ
 * 6.2). Image được ghim cứng {@code redis:7.4-alpine} → không phụ thuộc bản mới
 * nhất, và luôn >= 6.2.</p>
 */
@Testcontainers
class LeaderboardServiceRedisIntegrationTest {

    /**
     * Redis container — ghim version cứng.
     *
     * <p>Không dùng {@code redis:7-alpine} vì tag "7" có thể trỏ tới bản 7.x mới
     * hơn trong tương lai → test không reproducible.</p>
     */
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    private StringRedisTemplate redisTemplate;
    private LeaderboardService leaderboardService;

    /** Điểm tuyệt đối của user trong tuần — giả lập để test khỏi cần DB. */
    private LeaderboardScoreReader scoreReader;

    private static final String WEEK = "2026-W43";

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                REDIS.getHost(), REDIS.getMappedPort(6379));
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(factory);
        redisTemplate.afterPropertiesSet();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        scoreReader = mock(LeaderboardScoreReader.class);
        leaderboardService = new LeaderboardService(redisTemplate, scoreReader);
    }

    // =====================================================================
    // GT out-of-order — kịch bản 1
    // =====================================================================

    @Nested
    @DisplayName("ZADD GT — không bao giờ ghi lùi điểm")
    class AddGreaterThan {

        @Test
        @DisplayName("Ghi lùi thứ tự (10 → 11 → 10) thì ZSET giữ 11")
        void outOfOrderWrite_keepsHigherScore() {
            // T1 ghi 10
            leaderboardService.rebuild(WEEK, List.of(new LeaderboardRow(1L, 10)));
            // T2 ghi 11 (đi trước T1 gì đó) → có thứ tự ghi lại
            leaderboardService.rebuild(WEEK, List.of(new LeaderboardRow(1L, 11)));
            // T1 ghi lại 10 (ảnh chụp cũ) → ZADD GT phải chặn
            leaderboardService.rebuild(WEEK, List.of(new LeaderboardRow(1L, 10)));

            assertThat(leaderboardService.getScore(1L, WEEK)).isEqualTo(11L);
        }

        @Test
        @DisplayName("Ghi điểm cao hơn thì được ghi đè")
        void higherScoreOverwrites() {
            leaderboardService.rebuild(WEEK, List.of(new LeaderboardRow(1L, 10)));
            leaderboardService.rebuild(WEEK, List.of(new LeaderboardRow(1L, 25)));

            assertThat(leaderboardService.getScore(1L, WEEK)).isEqualTo(25L);
        }

        @Test
        @DisplayName("Ghi điểm thấp hơn thì bị bỏ qua (GT)")
        void lowerScoreIsRejected() {
            leaderboardService.rebuild(WEEK, List.of(new LeaderboardRow(1L, 25)));
            leaderboardService.rebuild(WEEK, List.of(new LeaderboardRow(1L, 10)));

            assertThat(leaderboardService.getScore(1L, WEEK)).isEqualTo(25L);
        }
    }

    // =====================================================================
    // Rebuild không đè bản mới hơn — kịch bản 2
    // =====================================================================

    @Nested
    @DisplayName("Rebuild không đè bản mới hơn")
    class RebuildKeepsNewer {

        @Test
        @DisplayName("Rebuild bằng ảnh chụp DB cũ không ghi đè điểm mới hơn")
        void rebuildWithStaleSnapshot_keepsNewerScore() {
            // ZSET đang có 20 (mới, từ syncUserScore sau commit)
            leaderboardService.rebuild(WEEK, List.of(new LeaderboardRow(1L, 20)));
            // Rebuild đọc ảnh chụp DB cũ lúc T0 → 15
            leaderboardService.rebuild(WEEK, List.of(new LeaderboardRow(1L, 15)));

            assertThat(leaderboardService.getScore(1L, WEEK)).isEqualTo(20L);
        }

        @Test
        @DisplayName("Hard rebuild xoá key rồi dựng lại → sửa được điểm giảm")
        void hardRebuild_canLowerScore() {
            leaderboardService.rebuild(WEEK, List.of(new LeaderboardRow(1L, 100)));
            assertThat(leaderboardService.getScore(1L, WEEK)).isEqualTo(100L);

            // hardRebuild: DEL key → ZADD lại → điểm 50 ghi đè được
            leaderboardService.hardRebuild(WEEK, List.of(new LeaderboardRow(1L, 50)));
            assertThat(leaderboardService.getScore(1L, WEEK)).isEqualTo(50L);

            // Và điểm cũ 100 phải biến mất (không còn member cũ)
            assertThat(leaderboardService.getTotalPlayers(WEEK)).isEqualTo(1L);
        }

        @Test
        @DisplayName("Hard rebuild xoá user không còn trong DB")
        void hardRebuild_removesStaleUsers() {
            leaderboardService.rebuild(WEEK, List.of(
                    new LeaderboardRow(1L, 100),
                    new LeaderboardRow(2L, 50)));
            assertThat(leaderboardService.getTotalPlayers(WEEK)).isEqualTo(2L);

            // DB giờ chỉ còn user 1
            leaderboardService.hardRebuild(WEEK, List.of(new LeaderboardRow(1L, 100)));
            assertThat(leaderboardService.getTotalPlayers(WEEK)).isEqualTo(1L);
            assertThat(leaderboardService.getScore(2L, WEEK)).isNull();
        }
    }

    // =====================================================================
    // Hạng — ZCOUNT exclusive
    // =====================================================================

    @Nested
    @DisplayName("Hạng — ZCOUNT exclusive")
    class Ranking {

        @Test
        @DisplayName("Cùng điểm → cùng hạng (1, 1, 3)")
        void sameScoreSameRank() {
            // Hai user cùng điểm CAO NHẤT (20) → cùng hạng 1
            // User điểm thấp hơn (10) → có 2 user cao hơn → hạng 3
            leaderboardService.rebuild(WEEK, List.of(
                    new LeaderboardRow(1L, 20),
                    new LeaderboardRow(2L, 20),
                    new LeaderboardRow(3L, 10)));

            assertThat(leaderboardService.getRankByScore(1L, WEEK, 20)).isEqualTo(1L);
            assertThat(leaderboardService.getRankByScore(2L, WEEK, 20)).isEqualTo(1L);
            // 3 (không phải 2) — vì 2 user cùng điểm đứng trên → "nhảy" hạng
            assertThat(leaderboardService.getRankByScore(3L, WEEK, 10)).isEqualTo(3L);
        }

        @Test
        @DisplayName("Hạng tính bằng số user có điểm CAO HƠN (exclusive)")
        void rankCountsOnlyHigherScores() {
            leaderboardService.rebuild(WEEK, List.of(
                    new LeaderboardRow(1L, 30),
                    new LeaderboardRow(2L, 20),
                    new LeaderboardRow(3L, 20),
                    new LeaderboardRow(4L, 10)));

            // 30 → không ai cao hơn → hạng 1
            assertThat(leaderboardService.getRankByScore(1L, WEEK, 30)).isEqualTo(1L);
            // 20 → có 1 user cao hơn (30) → hạng 2 (cả 2 user cùng điểm → cùng hạng)
            assertThat(leaderboardService.getRankByScore(2L, WEEK, 20)).isEqualTo(2L);
            assertThat(leaderboardService.getRankByScore(3L, WEEK, 20)).isEqualTo(2L);
            // 10 → có 3 user cao hơn → hạng 4
            assertThat(leaderboardService.getRankByScore(4L, WEEK, 10)).isEqualTo(4L);
        }

        @Test
        @DisplayName("Hạng khớp với thứ tự hiển thị trong bảng top")
        void rankMatchesDisplayOrder() {
            leaderboardService.rebuild(WEEK, List.of(
                    new LeaderboardRow(1L, 30),
                    new LeaderboardRow(2L, 20),
                    new LeaderboardRow(3L, 20),
                    new LeaderboardRow(4L, 10)));

            List<LeaderboardRow> top = leaderboardService.getTop(30, WEEK);
            assertThat(top).hasSize(4);
            // Thứ tự giảm dần theo score
            assertThat(top.get(0).score()).isEqualTo(30);
            assertThat(top.get(1).score()).isEqualTo(20);
            assertThat(top.get(2).score()).isEqualTo(20);
            assertThat(top.get(3).score()).isEqualTo(10);

            // Hạng của user theo bảng (cùng điểm → cùng hạng)
            // user 2 và 3 cùng 20 điểm, đứng ở vị trí 2 và 3 → cùng hạng 2
            assertThat(leaderboardService.getRankByScore(2L, WEEK, 20)).isEqualTo(2L);
            assertThat(leaderboardService.getRankByScore(3L, WEEK, 20)).isEqualTo(2L);
            // user 4 (10 điểm) đứng vị trí 4 → hạng 4
            assertThat(leaderboardService.getRankByScore(4L, WEEK, 10)).isEqualTo(4L);
        }
    }

    // =====================================================================
    // Top N & tổng số người
    // =====================================================================

    @Nested
    @DisplayName("Top N & tổng người")
    class TopAndTotals {

        @Test
        @DisplayName("Rebuild giữ user NGOÀI top 30 → ZCARD = tổng số user thật")
        void rebuildKeepsUsersOutsideTop30() {
            List<LeaderboardRow> all = new ArrayList<>();
            for (long i = 1; i <= 40; i++) {
                all.add(new LeaderboardRow(i, 100 - i));
            }
            leaderboardService.rebuild(WEEK, all);

            // ZCARD = 40 (không phải 30)
            assertThat(leaderboardService.getTotalPlayers(WEEK)).isEqualTo(40L);
            // getTop chỉ trả 30
            assertThat(leaderboardService.getTop(30, WEEK)).hasSize(30);
            // User hạng 40 vẫn còn trong ZSET → vẫn có hạng.
            // Score = 100 - i → user 40 có score 60.
            assertThat(leaderboardService.getScore(40L, WEEK)).isEqualTo(60L);
            assertThat(leaderboardService.getRankByScore(40L, WEEK, 60)).isEqualTo(40L);
        }

        @Test
        @DisplayName("getTop giữ thứ tự score giảm dần")
        void getTopIsSortedDescending() {
            leaderboardService.rebuild(WEEK, List.of(
                    new LeaderboardRow(1L, 10),
                    new LeaderboardRow(2L, 50),
                    new LeaderboardRow(3L, 30)));

            List<LeaderboardRow> top = leaderboardService.getTop(30, WEEK);
            assertThat(top).extracting(LeaderboardRow::userId)
                    .containsExactly(2L, 3L, 1L);
        }

        @Test
        @DisplayName("ZSET rỗng → getTop trả list rỗng, getTotalPlayers = 0")
        void emptySet() {
            assertThat(leaderboardService.getTop(30, WEEK)).isEmpty();
            assertThat(leaderboardService.getTotalPlayers(WEEK)).isEqualTo(0L);
            assertThat(leaderboardService.getScore(1L, WEEK)).isNull();
        }
    }

    // =====================================================================
    // syncUserScore
    // =====================================================================

    @Nested
    @DisplayName("syncUserScore")
    class SyncUserScore {

        @Test
        @DisplayName("Ghi điểm tuyệt đối đọc từ DB (idempotent, không cộng dồn)")
        void writesAbsoluteScoreFromDb() {
            when(scoreReader.readWeekScore(any(), any(), any())).thenReturn(30L);

            leaderboardService.syncUserScore(1L, WEEK);
            leaderboardService.syncUserScore(1L, WEEK);
            leaderboardService.syncUserScore(1L, WEEK);

            // Không phải 10, 20, 30 (cộng dồn) → mà luôn là 30 (giá trị tuyệt đối)
            assertThat(leaderboardService.getScore(1L, WEEK)).isEqualTo(30L);
        }

        @Test
        @DisplayName("Điểm 0 → KHÔNG tạo member (tránh nhiễu ZCARD/totalPlayers)")
        void zeroScoreDoesNotCreateMember() {
            when(scoreReader.readWeekScore(any(), any(), any())).thenReturn(0L);

            leaderboardService.syncUserScore(1L, WEEK);

            assertThat(leaderboardService.getScore(1L, WEEK)).isNull();
            assertThat(leaderboardService.getTotalPlayers(WEEK)).isEqualTo(0L);
        }

        @Test
        @DisplayName("Điểm âm cũng không tạo member")
        void negativeScoreDoesNotCreateMember() {
            when(scoreReader.readWeekScore(any(), any(), any())).thenReturn(-5L);

            leaderboardService.syncUserScore(1L, WEEK);

            assertThat(leaderboardService.getTotalPlayers(WEEK)).isEqualTo(0L);
        }
    }

    // =====================================================================
    // TTL
    // =====================================================================

    @Nested
    @DisplayName("TTL")
    class Ttl {

        @Test
        @DisplayName("Key tuần có TTL = 56 ngày (8 tuần)")
        void weekKeyHasEightWeekTtl() {
            leaderboardService.rebuild(WEEK, List.of(new LeaderboardRow(1L, 10)));

            Long ttl = redisTemplate.getExpire(leaderboardService.key(WEEK),
                    java.util.concurrent.TimeUnit.SECONDS);

            assertThat(ttl).isNotNull();
            // 56 ngày = 4_838_400 giây. Cho phép lệch nhỏ do thời gian chạy.
            assertThat(ttl).isBetween(Duration.ofDays(56).toSeconds() - 60,
                    Duration.ofDays(56).toSeconds());
        }

        @Test
        @DisplayName("Hard rebuild xoá key rồi dựng lại → TTL được đặt lại")
        void hardRebuildResetsTtl() {
            leaderboardService.rebuild(WEEK, List.of(new LeaderboardRow(1L, 10)));
            leaderboardService.hardRebuild(WEEK, List.of(new LeaderboardRow(1L, 50)));

            Long ttl = redisTemplate.getExpire(leaderboardService.key(WEEK),
                    java.util.concurrent.TimeUnit.SECONDS);
            assertThat(ttl).isNotNull();
            assertThat(ttl).isBetween(Duration.ofDays(56).toSeconds() - 60,
                    Duration.ofDays(56).toSeconds());
        }
    }

    // =====================================================================
    // Lock & cooldown
    // =====================================================================

    @Nested
    @DisplayName("Lock & cooldown")
    class LocksAndCooldowns {

        @Test
        @DisplayName("Rebuild lock chỉ 1 người giành được")
        void rebuildLockIsExclusive() {
            assertThat(leaderboardService.tryAcquireRebuildLock(WEEK)).isTrue();
            assertThat(leaderboardService.tryAcquireRebuildLock(WEEK)).isFalse();
        }

        @Test
        @DisplayName("Refresh cooldown chặn lần gọi thứ 2")
        void refreshCooldownBlocksSecondCall() {
            assertThat(leaderboardService.acquireRefreshCooldown(WEEK)).isTrue();
            assertThat(leaderboardService.acquireRefreshCooldown(WEEK)).isFalse();
        }

        @Test
        @DisplayName("Lock của tuần khác không ảnh hưởng")
        void locksArePerWeek() {
            assertThat(leaderboardService.tryAcquireRebuildLock("2026-W43")).isTrue();
            assertThat(leaderboardService.tryAcquireRebuildLock("2026-W42")).isTrue();
        }
    }

    // =====================================================================
    // Fail-safe
    // =====================================================================

    @Nested
    @DisplayName("Fail-safe — Redis lỗi không làm hỏng luồng chính")
    class FailSafe {

        @Test
        @DisplayName("syncUserScore nuốt exception khi Redis chết")
        void syncUserScoreSwallowsRedisFailure() {
            StringRedisTemplate broken = mock(StringRedisTemplate.class);
            when(broken.execute(any(org.springframework.data.redis.core.RedisCallback.class)))
                    .thenThrow(new RuntimeException("Redis down"));

            LeaderboardService service = new LeaderboardService(broken, scoreReader);

            // Không ném exception ra ngoài
            assertThatCode(() -> service.syncUserScore(1L, WEEK))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("getTop / getScore / getTotalPlayers trả giá trị an toàn khi Redis lỗi")
        void readMethodsDegradeGracefully() {
            StringRedisTemplate broken = mock(StringRedisTemplate.class);
            when(broken.execute(any(org.springframework.data.redis.core.RedisCallback.class)))
                    .thenThrow(new RuntimeException("Redis down"));

            LeaderboardService service = new LeaderboardService(broken, scoreReader);

            assertThat(service.getTop(30, WEEK)).isEmpty();
            assertThat(service.getScore(1L, WEEK)).isNull();
            assertThat(service.getTotalPlayers(WEEK)).isNull();
            assertThat(service.getRankByScore(1L, WEEK, 10)).isNull();
        }
    }
}
