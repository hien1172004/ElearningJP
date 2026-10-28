package com.jp.elearningjp.service.progress;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.connection.zset.Tuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Bảng xếp hạng tuần dùng <b>Redis Sorted Set</b> — ZSET là <b>index dẫn xuất từ DB</b>,
 * không phải nguồn sự thật.
 *
 * <h2>Nguồn sự thật</h2>
 * <p>DB ({@code user_daily_stats}) là nguồn sự thật. Điểm =
 * {@code new_items_learned * learned_item_points + lessons_completed * lesson_points}.</p>
 *
 * <h2>Vì sao bắt buộc dùng {@code ZADD GT}?</h2>
 * <p>ZSET luôn được đồng bộ bằng <b>{@code ZADD} với {@code GT}</b> (chỉ ghi khi score
 * mới <b>lớn hơn</b> score đang có) → ZSET <b>không bao giờ ghi lùi điểm</b>, kể cả khi
 * rebuild dùng ảnh chụp DB cũ xen kẽ với ghi điểm. Ba kịch bản sai thứ tự mà
 * {@code ZADD} thường sẽ sai:</p>
 * <ol>
 *   <li><b>Hai transaction cùng user ghi sai thứ tự.</b> T1 đọc score = 10, T2 đọc
 *       score = 11 và ghi trước, T1 ghi sau → {@code ZADD} thường sẽ ghi lùi về 10,
 *       lệch DB. {@code GT} chặn điều này.</li>
 *   <li><b>Rebuild dùng ảnh chụp DB cũ.</b> Rebuild đọc DB lúc T0, xong việc đọc thì
 *       T1 commit tăng điểm và {@code syncUserScore} ghi score mới; rebuild sau đó
 *       {@code ZADD} ảnh chụp cũ sẽ đè mất. {@code GT} chặn điều này.</li>
 *   <li><b>RENAME không bảo toàn increment.</b> Đã loại bỏ RENAME hoàn toàn.</li>
 * </ol>
 *
 * <p><b>Hệ quả phải chấp nhận:</b> vì {@code GT} chỉ cho phép tăng, nếu DB bị sửa điểm
 * xuống (xoá row {@code user_daily_stats}, sửa dữ liệu) thì ZSET <b>không tự sửa</b>.
 * Cách khắc phục: gọi {@link #hardRebuild} (xoá key rồi dựng lại từ đầu).</p>
 *
 * <h2>Vì sao dùng {@link StringRedisTemplate} + connection-level?</h2>
 * <ul>
 *   <li>{@code RedisTemplate<String,Object>} serialize value/member bằng
 *       {@code GenericJackson2JsonRedisSerializer} <b>có default typing</b> → member
 *       {@code "42"} thực tế lưu thành {@code ["java.lang.String","42"]}. Chuyển sang
 *       {@link StringRedisTemplate} để member là chuỗi thuần. Vì key cũ chứa member
 *       theo encoding cũ nên <b>prefix key được đổi</b> sang {@code v2} (nay là
 *       {@code v3} sau khi đổi ngữ nghĩa điểm sang từ mới đã học).</li>
 *   <li>{@code ZSetOperations} <b>không có</b> overload {@code GT} cho {@code add(...)}.
 *       {@code GT} chỉ có ở connection level:
 *       {@code RedisZSetCommands.zAdd(byte[], double, byte[], ZAddArgs)} với
 *       {@code ZAddArgs.gt()}.</li>
 *   <li>{@code ZSetOperations.count(k, min, max)} map sang {@code ZCOUNT key min max} —
 *       <b>inclusive cả hai đầu</b> → đếm cả member <b>cùng score</b> → hạng sai.
 *       Phải dùng {@code RedisZSetCommands.zCount(byte[], Range)} với
 *       {@code Range.open(score, +inf)} (exclusive).</li>
 * </ul>
 *
 * <p><b>Yêu cầu phiên bản Redis:</b> cú pháp exclusive range {@code (10} chỉ có từ
 * <b>Redis 6.2</b>. Bản cũ hơn sẽ <b>parse lỗi</b> → mất hạng. Đang dùng
 * {@code redis:7-alpine} → OK.</p>
 *
 * <p><b>Fail-safe:</b> mọi lỗi Redis đều được nuốt, caller fallback sang query DB.
 * Lỗi trong {@code afterCommit} không thể rollback DB (tx đã commit) nhưng
 * <b>không được</b> để exception nổi lên làm fail request.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    /**
     * Prefix key của Sorted Set.
     *
     * <p><b>v3</b> vì <b>ngữ nghĩa điểm đổi</b>: trước đây score =
     * {@code lookups_scored * lookup_points + lessons_completed * lesson_points},
     * nay score = {@code new_items_learned * learned_item_points + lessons_completed * lesson_points}.
     * Key v2 chứa điểm theo công thức cũ nên không thể tái sử dụng → bump lên v3,
     * key v2 tự hết TTL.</p>
     */
    private static final String KEY_PREFIX = "leaderboard:weekly:v3:";

    /** Prefix key khoá rebuild (chống cache stampede). */
    private static final String REBUILD_LOCK_PREFIX = "leaderboard:rebuild:lock:";

    /** Prefix key cooldown refresh (chống spam nút Reload). */
    private static final String REFRESH_CD_PREFIX = "leaderboard:refresh:cd:";

    /** TTL của key tuần = 8 tuần = 56 ngày. */
    private static final Duration WEEK_TTL = Duration.ofDays(56);

    /** TTL khoá rebuild = 30s (tuần đông người cần đọc toàn bộ + ZADD batch). */
    private static final Duration REBUILD_LOCK_TTL = Duration.ofSeconds(30);

    /** TTL cooldown refresh = 30s. */
    private static final Duration REFRESH_COOLDOWN_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private final LeaderboardScoreReader scoreReader;

    // =====================================================================
    // Write
    // =====================================================================

    /**
     * Đồng bộ điểm xếp hạng của user trong tuần vào Redis Sorted Set.
     *
     * <p><b>Thay cho {@code incrementScore}</b>: đọc điểm <b>tuyệt đối</b> của user
     * trong tuần từ DB → {@code ZADD GT} → {@code EXPIRE}. Idempotent, chạy sau commit.</p>
     *
     * <p>Vì {@code GT} chỉ tăng: nếu DB score = 0 (user chưa có điểm) → <b>không</b> ghi
     * (tránh tạo member score 0 làm nhiễu {@code ZCARD}/{@code totalPlayers}).</p>
     *
     * <p><b>Bắt exception, chỉ log</b> — tuyệt đối không làm hỏng luồng tra từ /
     * hoàn thành bài. Bọc try/catch trong chính method này (không chỉ ở caller) để
     * mọi nơi gọi đều an toàn.</p>
     *
     * <p><b>Quan trọng — đọc DB sau commit:</b> {@code afterCommit} chạy sau khi tx đã
     * commit nhưng connection gốc vẫn còn bind trên thread. Query đọc score chạy
     * trong {@code REQUIRES_NEW} (xem {@link LeaderboardScoreReader}) → connection
     * mới, snapshot mới.</p>
     *
     * @param userId  user cần đồng bộ
     * @param weekKey tuần (ISO: {@code YYYY-Www})
     */
    public void syncUserScore(Long userId, String weekKey) {
        if (userId == null) {
            return;
        }
        try {
            LocalDate[] range = LeaderboardWeekUtil.weekRange(weekKey);
            long score = scoreReader.readWeekScore(userId, range[0], range[1]);
            if (score <= 0) {
                // Không ghi score 0 → không tạo member rỗng làm nhiễu ZCARD.
                log.debug("[Leaderboard] skip sync user={} week={} (score=0)", userId, weekKey);
                return;
            }
            boolean added = zAddGt(key(weekKey), userId, score);
            if (added) {
                applyTtl(key(weekKey));
            }
            log.debug("[Leaderboard] ZADD GT user={} score={} week={} (applied={})",
                    userId, score, weekKey, added);
        } catch (Exception ex) {
            log.warn("[Leaderboard] syncUserScore failed for user {} week {}: {}",
                    userId, weekKey, ex.getMessage());
        }
    }

    /**
     * {@code ZADD GT} cho 1 member — chỉ ghi khi score mới lớn hơn score hiện có.
     *
     * @return {@code true} nếu score mới được ghi (mới hơn)
     */
    private boolean zAddGt(String redisKey, Long userId, long score) {
        Boolean result = redisTemplate.execute((RedisCallback<Boolean>) conn ->
                conn.zSetCommands().zAdd(
                        bytes(redisKey),
                        score,
                        bytes(userId.toString()),
                        RedisZSetCommands.ZAddArgs.empty().gt()));
        return Boolean.TRUE.equals(result);
    }

    // =====================================================================
    // Read
    // =====================================================================

    /**
     * Lấy top N user của 1 tuần, sắp xếp theo score giảm dần.
     *
     * <p>Dùng {@code ZREVRANGE key 0 topN-1 WITHSCORES}.</p>
     *
     * @param topN   số lượng user (VD: 5)
     * @param weekKey tuần cần đọc
     * @return danh sách {@code (userId, score)} giữ thứ tự; empty nếu Redis miss / lỗi
     */
    public List<LeaderboardRow> getTop(int topN, String weekKey) {
        if (topN <= 0) {
            return List.of();
        }
        try {
            Set<Tuple> raw = redisTemplate.execute((RedisCallback<Set<Tuple>>) conn ->
                    conn.zSetCommands().zRevRangeWithScores(bytes(key(weekKey)), 0, topN - 1));

            if (raw == null || raw.isEmpty()) {
                return List.of();
            }
            List<LeaderboardRow> result = new ArrayList<>(raw.size());
            for (Tuple tuple : raw) {
                Long userId = parseUserId(tuple.getValue());
                Double score = tuple.getScore();
                if (userId != null && score != null) {
                    result.add(new LeaderboardRow(userId, score.longValue()));
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("[Leaderboard] getTop failed week={}: {}", weekKey, ex.getMessage());
            return List.of();
        }
    }

    /**
     * Tính hạng của user trong tuần.
     *
     * <p>Công thức: {@code ZCOUNT(score, +inf) + 1} với {@code Range.open} (exclusive).
     * Đếm member có score <b>lớn hơn</b> score của tôi → rank = 1 + số đó → đúng
     * nghĩa "cùng điểm thì cùng hạng" (1, 1, 3).</p>
     *
     * <p><b>Không dùng {@code ZREVRANK}</b> (Redis xếp hạng khác nhau cho member đồng
     * điểm).</p>
     *
     * <p><b>Vì sao nhận {@code score} từ bên ngoài?</b> Caller thường đã có sẵn
     * điểm (từ {@link #getScore}) và cần cả hạng. Nếu method tự gọi {@code ZSCORE}
     * lần nữa thì mỗi lần hiển thị hạng cá nhân tốn thêm 1 roundtrip thừa.
     * Nhận score làm tham số để caller kiểm soát số lần đọc.</p>
     *
     * @return rank bắt đầu từ 1, hoặc {@code null} nếu Redis lỗi
     */
    public Long getRankByScore(Long userId, String weekKey, long score) {
        if (userId == null) {
            return null;
        }
        try {
            Long greater = countGreaterThan(key(weekKey), score);
            return greater == null ? null : greater + 1;
        } catch (Exception ex) {
            log.warn("[Leaderboard] getRankByScore failed user={} week={}: {}",
                    userId, weekKey, ex.getMessage());
            return null;
        }
    }

    /**
     * Đếm số member có score <b>lớn hơn</b> {@code score} — tương đương
     * {@code ZCOUNT (score, +inf)}.
     *
     * <p><b>Cần Redis >= 6.2</b>: cú pháp exclusive {@code "(score"} chỉ có từ 6.2.
     * Redis cũ sẽ parse lỗi → exception → caller fallback sang DB.</p>
     */
    private Long countGreaterThan(String redisKey, double score) {
        return redisTemplate.execute((RedisCallback<Long>) conn ->
                conn.zSetCommands().zCount(
                        bytes(redisKey),
                        Range.open(score, Double.POSITIVE_INFINITY)));
    }

    /**
     * Lấy score của user trong tuần.
     */
    public Long getScore(Long userId, String weekKey) {
        if (userId == null) {
            return null;
        }
        try {
            Double score = redisTemplate.execute((RedisCallback<Double>) conn ->
                    conn.zSetCommands().zScore(bytes(key(weekKey)), bytes(userId.toString())));
            return score == null ? null : score.longValue();
        } catch (Exception ex) {
            log.warn("[Leaderboard] getScore failed user={} week={}: {}",
                    userId, weekKey, ex.getMessage());
            return null;
        }
    }

    /**
     * Tổng số user có trong bảng xếp hạng tuần ({@code ZCARD}).
     */
    public Long getTotalPlayers(String weekKey) {
        try {
            Long card = redisTemplate.execute((RedisCallback<Long>) conn ->
                    conn.zSetCommands().zCard(bytes(key(weekKey))));
            return card == null ? 0L : card;
        } catch (Exception ex) {
            log.warn("[Leaderboard] getTotalPlayers failed week={}: {}", weekKey, ex.getMessage());
            return null;
        }
    }

    // =====================================================================
    // Rebuild
    // =====================================================================

    /**
     * Rebuild Sorted Set của 1 tuần từ dữ liệu DB.
     *
     * <p><b>Cũng dùng {@code ZADD GT}</b> → không đè bản mới hơn bằng ảnh chụp DB cũ.</p>
     *
     * <p><b>Không dùng {@code ZREMRANGEBYSCORE ≤ 0}</b> — không cần:
     * {@code findAllScoresByWeek} chỉ trả user có score &gt; 0, và {@code GT} đảm bảo
     * không ghi lùi.</p>
     *
     * <p><b>Trade-off:</b> user đã bị xoá {@code daily_stats} trong tuần đó sẽ không
     * được dọn khỏi ZSET (vì {@code GT} không cho giảm). Cách khắc phục:
     * {@link #hardRebuild}.</p>
     *
     * @param weekKey  tuần cần rebuild
     * @param dbScores điểm tuyệt đối từ DB ({@code [userId, score]})
     */
    public void rebuild(String weekKey, List<LeaderboardRow> dbScores) {
        if (dbScores == null || dbScores.isEmpty()) {
            log.info("[Leaderboard] rebuild skipped week={} (no data)", weekKey);
            return;
        }
        String redisKey = key(weekKey);
        try {
            Set<Tuple> tuples = new LinkedHashSet<>(dbScores.size());
            for (LeaderboardRow row : dbScores) {
                if (row == null || row.score() <= 0) {
                    continue;
                }
                tuples.add(Tuple.of(bytes(row.userId().toString()), (double) row.score()));
            }
            if (tuples.isEmpty()) {
                return;
            }
            Long added = redisTemplate.execute((RedisCallback<Long>) conn ->
                    conn.zSetCommands().zAdd(
                            bytes(redisKey), tuples, RedisZSetCommands.ZAddArgs.empty().gt()));
            applyTtl(redisKey);
            log.info("[Leaderboard] rebuilt week={} users={} (added={})", weekKey, tuples.size(), added);
        } catch (Exception ex) {
            log.warn("[Leaderboard] rebuild failed week={}: {}", weekKey, ex.getMessage());
        }
    }

    /**
     * Rebuild <b>từ đầu</b>: xoá key rồi dựng lại — dùng khi cần <b>sửa điểm giảm</b>
     * (sửa tay dữ liệu trong DB, xoá row {@code user_daily_stats}).
     *
     * <p>Khác với {@link #rebuild}, method này <b>xoá</b> dữ liệu cũ nên có thể sửa
     * điểm giảm — điều mà {@code ZADD GT} không làm được.</p>
     *
     * <p><b>Phải giữ lock rebuild suốt thời gian DEL + rebuild</b> → giữa hai bước bảng
     * sẽ trống, nhưng request thua lock <b>đọc DB và trả về luôn</b>, không đọc ZSET
     * rỗng → không ai thấy bảng trống.</p>
     *
     * <p>Sau khi DEL, phải {@code EXPIRE} lại key (đã xoá nên TTL mất).</p>
     *
     * @param weekKey  tuần cần rebuild
     * @param dbScores điểm tuyệt đối từ DB ({@code [userId, score]})
     */
    public void hardRebuild(String weekKey, List<LeaderboardRow> dbScores) {
        String redisKey = key(weekKey);
        try {
            redisTemplate.delete(redisKey);
            log.info("[Leaderboard] hardRebuild: deleted key week={}", weekKey);

            if (dbScores == null || dbScores.isEmpty()) {
                log.info("[Leaderboard] hardRebuild done week={} (no data)", weekKey);
                return;
            }
            Set<Tuple> tuples = new LinkedHashSet<>(dbScores.size());
            for (LeaderboardRow row : dbScores) {
                if (row == null || row.score() <= 0) {
                    continue;
                }
                tuples.add(Tuple.of(bytes(row.userId().toString()), (double) row.score()));
            }
            if (tuples.isEmpty()) {
                return;
            }
            Long added = redisTemplate.execute((RedisCallback<Long>) conn ->
                    conn.zSetCommands().zAdd(
                            bytes(redisKey), tuples, RedisZSetCommands.ZAddArgs.empty().gt()));
            applyTtl(redisKey);
            log.info("[Leaderboard] hardRebuild done week={} users={} (added={})",
                    weekKey, tuples.size(), added);
        } catch (Exception ex) {
            log.warn("[Leaderboard] hardRebuild failed week={}: {}", weekKey, ex.getMessage());
        }
    }

    // =====================================================================
    // Lock / cooldown
    // =====================================================================

    /**
     * Thử giành khoá rebuild — chống <b>cache stampede</b>: nhiều request thấy ZSET
     * rỗng cùng chạy rebuild.
     *
     * <p>Request thắng khoá rebuild; request thua <b>đọc DB và trả về luôn, không ghi
     * Redis</b> (tránh ghi đè kết quả của request thắng).</p>
     *
     * <p>TTL <b>30s</b> (không phải 10s): tuần đông người cần đọc toàn bộ bảng +
     * ZADD batch.</p>
     *
     * @return {@code true} nếu giành được khoá
     */
    public boolean tryAcquireRebuildLock(String weekKey) {
        try {
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(REBUILD_LOCK_PREFIX + weekKey, "1", REBUILD_LOCK_TTL);
            return Boolean.TRUE.equals(ok);
        } catch (Exception ex) {
            log.warn("[Leaderboard] acquire rebuild lock failed week={}: {}", weekKey, ex.getMessage());
            // Redis lỗi → cho qua (coi như không có khoá) để không chặn người dùng.
            return true;
        }
    }

    /**
     * Thử giành cooldown refresh — chống spam nút "Reload".
     *
     * @return {@code true} nếu được phép refresh; {@code false} nếu còn trong cooldown
     */
    public boolean acquireRefreshCooldown(String weekKey) {
        try {
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(REFRESH_CD_PREFIX + weekKey, "1", REFRESH_COOLDOWN_TTL);
            return Boolean.TRUE.equals(ok);
        } catch (Exception ex) {
            log.warn("[Leaderboard] acquire refresh cooldown failed week={}: {}", weekKey, ex.getMessage());
            // Redis lỗi → cho qua, không chặn người dùng vì lỗi hạ tầng.
            return true;
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Tạo key Redis của 1 tuần. */
    public String key(String weekKey) {
        return KEY_PREFIX + weekKey;
    }

    /**
     * Đặt TTL cho key tuần — TTL đặt <b>ngay khi tạo</b> để không rò rỉ nếu lỗi giữa chừng.
     */
    private void applyTtl(String redisKey) {
        try {
            redisTemplate.expire(redisKey, WEEK_TTL.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.warn("[Leaderboard] apply TTL failed key={}: {}", redisKey, ex.getMessage());
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static Long parseUserId(byte[] raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(new String(raw, StandardCharsets.UTF_8));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Một dòng của bảng xếp hạng: user + điểm.
     *
     * @param userId user
     * @param score  điểm xếp hạng (điểm tuyệt đối trong tuần)
     */
    public record LeaderboardRow(Long userId, long score) {
    }
}
