package com.jp.elearningjp.service.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jp.elearningjp.dto.response.progress.ProgressSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Cache layer cho trang Tiến độ học tập (Redis).
 *
 * <p><b>Vì sao dùng {@code RedisTemplate} thủ công thay vì {@code @Cacheable}?</b></p>
 * <ul>
 *   <li>Cache key động ({@code from}/{@code to}) → SpEL rườm rà, khó đọc</li>
 *   <li>Cần xóa <b>tất cả</b> cache của 1 user khi có write — {@code @CacheEvict}
 *       không hỗ trợ xóa theo pattern/wildcard</li>
 *   <li>Tránh phụ thuộc {@code GenericJackson2JsonRedisSerializer} + default typing</li>
 * </ul>
 *
 * <p><b>Chiến lược invalidate — generation token:</b></p>
 * <pre>
 *   user ghi dữ liệu mới
 *        ↓
 *   invalidate(userId) → gen = "uuid-mới"
 *        ↓
 *   cache key = progress:summary:{userId}:{from}:{to}:{gen}
 *   cache cũ có gen cũ → không còn ai đọc → tự hết hạn theo TTL
 * </pre>
 * <p>Đây là O(1): không cần {@code SCAN} toàn bộ keyspace.</p>
 *
 * <p><b>Fail-safe:</b> mọi lỗi Redis đều bị nuốt và fallback sang đọc DB.
 * Redis chết không được làm chết trang Progress.</p>
 */
@Service
@Slf4j
public class ProgressCacheService {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final String PREFIX_SUMMARY = "progress:summary:";
    private static final String PREFIX_CALENDAR = "progress:calendar:";
    private static final String PREFIX_CHART = "progress:chart:";
    private static final String PREFIX_GEN = "progress:gen:";

    /**
     * TTL ngắn vì summary thay đổi liên tục khi user đang học.
     * Generation bump đã đảm bảo tính đúng; TTL chỉ để dọn rác.
     */
    private static final Duration TTL_SUMMARY = Duration.ofMinutes(5);
    private static final Duration TTL_CALENDAR = Duration.ofMinutes(30);
    private static final Duration TTL_CHART = Duration.ofMinutes(15);

    /** TTL generation token — chỉ cần sống lâu hơn cache con. */
    private static final Duration TTL_GEN = Duration.ofHours(24);

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper cacheObjectMapper;

    public ProgressCacheService(
            @Qualifier("redisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        // ObjectMapper RIÊNG cho cache:
        //  - không ảnh hưởng primary mapper của Web MVC
        //  - KHÔNG bật default typing (an toàn khi deserialize dữ liệu từ Redis)
        this.cacheObjectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // =========================================================================
    // GENERATION — cơ chế invalidate O(1)
    // =========================================================================

    /**
     * Lấy generation hiện tại của user, tạo mới nếu chưa có.
     *
     * <p>Trả về {@code "bypass"} khi Redis lỗi → cache key luôn khác nhau mỗi lần
     * → tự động bỏ qua cache (fail-safe, không sập).</p>
     */
    public String currentGeneration(Long userId) {
        String key = PREFIX_GEN + userId;
        try {
            Object gen = redisTemplate.opsForValue().get(key);
            if (gen != null) {
                return String.valueOf(gen);
            }
            String fresh = UUID.randomUUID().toString();
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, fresh, TTL_GEN);
            if (Boolean.TRUE.equals(ok)) {
                return fresh;
            }
            // Request song song đã set trước → đọc lại giá trị họ tạo.
            Object after = redisTemplate.opsForValue().get(key);
            return after != null ? String.valueOf(after) : fresh;
        } catch (Exception ex) {
            log.warn("[ProgressCache] get generation fail, bypass cache: {}", ex.getMessage());
            return "bypass";
        }
    }

    /**
     * Bump generation → vô hiệu toàn bộ cache Progress của user.
     *
     * <p>Gọi mỗi khi có write vào {@code user_daily_stats}.</p>
     */
    public void invalidate(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    PREFIX_GEN + userId, UUID.randomUUID().toString(), TTL_GEN);
            log.debug("[ProgressCache] invalidated user={}", userId);
        } catch (Exception ex) {
            log.warn("[ProgressCache] invalidate fail (ignored): {}", ex.getMessage());
        }
    }

    // =========================================================================
    // KHỐI 1 — SUMMARY
    // =========================================================================

    /**
     * Đọc summary từ cache.
     *
     * @return {@code null} nếu miss / lỗi Redis (caller phải đọc DB)
     */
    public ProgressSummaryResponse getSummary(Long userId, LocalDate from, LocalDate to) {
        try {
            Object raw = redisTemplate.opsForValue().get(summaryKey(userId, from, to));
            return raw == null ? null : deserialize(raw, ProgressSummaryResponse.class);
        } catch (Exception ex) {
            log.warn("[ProgressCache] get summary fail: {}", ex.getMessage());
            return null;
        }
    }

    public void putSummary(Long userId, LocalDate from, LocalDate to,
                           ProgressSummaryResponse value) {
        try {
            redisTemplate.opsForValue().set(
                    summaryKey(userId, from, to), serialize(value), TTL_SUMMARY);
        } catch (Exception ex) {
            log.warn("[ProgressCache] put summary fail (ignored): {}", ex.getMessage());
        }
    }

    // =========================================================================
    // KHỐI 2 — CALENDAR (Bước 5)
    // =========================================================================

    /** Đọc calendar từ cache. Trả {@code null} nếu miss. */
    public <T> List<T> getCalendar(Long userId, String month, Class<T> itemType) {
        try {
            return readList(calendarKey(userId, month), itemType);
        } catch (Exception ex) {
            log.warn("[ProgressCache] get calendar fail: {}", ex.getMessage());
            return null;
        }
    }

    public void putCalendar(Long userId, String month, List<?> value) {
        try {
            redisTemplate.opsForValue().set(
                    calendarKey(userId, month), serialize(value), TTL_CALENDAR);
        } catch (Exception ex) {
            log.warn("[ProgressCache] put calendar fail (ignored): {}", ex.getMessage());
        }
    }

    // =========================================================================
    // KHỐI 3 — CHART (Bước 6)
    // =========================================================================

    public <T> List<T> getChart(Long userId, String from, String to, Class<T> itemType) {
        try {
            return readList(chartKey(userId, from, to), itemType);
        } catch (Exception ex) {
            log.warn("[ProgressCache] get chart fail: {}", ex.getMessage());
            return null;
        }
    }

    public void putChart(Long userId, String from, String to, List<?> value) {
        try {
            redisTemplate.opsForValue().set(
                    chartKey(userId, from, to), serialize(value), TTL_CHART);
        } catch (Exception ex) {
            log.warn("[ProgressCache] put chart fail (ignored): {}", ex.getMessage());
        }
    }

    // =========================================================================
    // Key builders
    // =========================================================================

    private String summaryKey(Long userId, LocalDate from, LocalDate to) {
        return PREFIX_SUMMARY + userId + ":" + from + ":" + to + ":" + currentGeneration(userId);
    }

    private String calendarKey(Long userId, String month) {
        return PREFIX_CALENDAR + userId + ":" + month + ":" + currentGeneration(userId);
    }

    private String chartKey(Long userId, String from, String to) {
        return PREFIX_CHART + userId + ":" + from + ":" + to + ":" + currentGeneration(userId);
    }

    // =========================================================================
    // Helpers — serialize / deserialize
    // =========================================================================

    private String serialize(Object value) throws Exception {
        return cacheObjectMapper.writeValueAsString(value);
    }

    private <T> T deserialize(Object raw, Class<T> clazz) throws Exception {
        if (raw instanceof String json) {
            return cacheObjectMapper.readValue(json, clazz);
        }
        if (clazz.isInstance(raw)) {
            return clazz.cast(raw);
        }
        throw new IllegalStateException("Unexpected cached type: " + raw.getClass());
    }

    /**
     * Đọc list từ cache với kiểu phần tử cụ thể.
     *
     * <p>Vì không thể dùng {@code TypeReference} động dễ dàng, ta parse từng phần tử
     * riêng — chậm hơn một chút nhưng cache hit là đường đọc nóng, không sao.</p>
     */
    private <T> List<T> readList(String key, Class<T> itemType) throws Exception {
        Object raw = redisTemplate.opsForValue().get(key);
        if (raw == null) {
            return null;
        }
        List<Object> parsed;
        if (raw instanceof String json) {
            parsed = cacheObjectMapper.readValue(json, List.class);
        } else if (raw instanceof List<?> list) {
            parsed = new ArrayList<>(list);
        } else {
            return null;
        }

        List<T> result = new ArrayList<>(parsed.size());
        for (Object item : parsed) {
            result.add(deserialize(item, itemType));
        }
        return result;
    }

    // =========================================================================
    // Maintenance — chỉ dùng cho debug/admin
    // =========================================================================

    /**
     * Xóa toàn bộ key Progress của user (kể cả key generation).
     *
     * <p>Chỉ dùng khi cần dọn thủ công. Luôn ưu tiên {@link #invalidate(Long)}
     * vì generation bump O(1), còn hàm này phải SCAN keyspace (O(N)).</p>
     */
    public int purgeAll(Long userId) {
        try {
            List<String> targets = new ArrayList<>();
            collectKeys(PREFIX_SUMMARY + userId + ":*", targets);
            collectKeys(PREFIX_CALENDAR + userId + ":*", targets);
            collectKeys(PREFIX_CHART + userId + ":*", targets);
            targets.add(PREFIX_GEN + userId);

            if (targets.isEmpty()) {
                return 0;
            }
            Long deleted = redisTemplate.delete(targets);
            log.info("[ProgressCache] purgeAll user={} deleted={}", userId, deleted);
            return deleted != null ? deleted.intValue() : 0;
        } catch (Exception ex) {
            log.warn("[ProgressCache] purgeAll fail: {}", ex.getMessage());
            return 0;
        }
    }

    private void collectKeys(String pattern, List<String> out) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                out.addAll(keys);
            }
        } catch (Exception ex) {
            log.warn("[ProgressCache] SCAN {} fail: {}", pattern, ex.getMessage());
        }
    }
}