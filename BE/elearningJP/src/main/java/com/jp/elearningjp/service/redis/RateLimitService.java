package com.jp.elearningjp.service.redis;

import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Giới hạn số lần gọi theo cửa sổ cố định (fixed window) trên Redis.
 *
 * <p>Mỗi key là một bộ đếm tự hết hạn sau {@code window}: request đầu tiên tạo key + đặt TTL,
 * các request sau chỉ tăng đếm (không kéo dài TTL). Hết TTL → Redis tự xóa → được gọi lại từ đầu.</p>
 *
 * <p>Redis lỗi → cho qua (fail-open) + log, để Redis chết không kéo theo việc không đăng nhập được.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    static final String PREFIX = "rl:";

    /** INCR + đặt TTL ở lần đầu trong 1 lệnh atomic — tránh key không có TTL (khóa vĩnh viễn) nếu chết giữa chừng. */
    private static final RedisScript<Long> INCR_WITH_TTL = RedisScript.of("""
            local n = redis.call('INCR', KEYS[1])
            if n == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return n
            """, Long.class);

    private final StringRedisTemplate redis;

    /** Tăng đếm; vượt {@code max} trong cửa sổ → ném {@link ErrorCode#TOO_MANY_REQUESTS}. */
    public void check(String key, int max, Duration window) {
        if (!tryAcquire(key, max, window)) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    /** Như {@link #check} nhưng trả false thay vì ném lỗi (dùng khi phải trả thành công giả, vd quên mật khẩu). */
    public boolean tryAcquire(String key, int max, Duration window) {
        Long n = incr(key, window);
        return n == null || n <= max;
    }

    /** Chỉ đọc, không tăng: bộ đếm đã chạm {@code max} chưa. */
    public boolean isOverLimit(String key, int max) {
        try {
            String v = redis.opsForValue().get(PREFIX + key);
            return v != null && Long.parseLong(v) >= max;
        } catch (Exception e) {
            log.warn("Rate limit tạm tắt (đọc {} lỗi): {}", key, e.getMessage());
            return false;
        }
    }

    /** Tăng đếm, không ném lỗi (vd đếm lần đăng nhập sai). */
    public void hit(String key, Duration window) {
        incr(key, window);
    }

    public void reset(String key) {
        try {
            redis.delete(PREFIX + key);
        } catch (Exception e) {
            log.warn("Không xóa được bộ đếm {}: {}", key, e.getMessage());
        }
    }

    /** @return số lần hiện tại, hoặc null nếu Redis lỗi (fail-open). */
    private Long incr(String key, Duration window) {
        try {
            return redis.execute(INCR_WITH_TTL, List.of(PREFIX + key), String.valueOf(window.toMillis()));
        } catch (Exception e) {
            log.warn("Rate limit tạm tắt (Redis lỗi ở {}): {}", key, e.getMessage());
            return null;
        }
    }
}
