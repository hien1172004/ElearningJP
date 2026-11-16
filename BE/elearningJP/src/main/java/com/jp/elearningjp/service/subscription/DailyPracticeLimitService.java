package com.jp.elearningjp.service.subscription;

import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Quản lý hạn mức xuất phiếu / vở luyện viết chữ Hán và từ vựng theo ngày.
 *
 * <p>Quy tắc nghiệp vụ:</p>
 * <ul>
 *   <li>Tài khoản FREE: Tối đa 10 từ/ngày, tự động reset vào 00:00 ngày hôm sau.</li>
 *   <li>Tài khoản PRO: Không giới hạn (unlimited).</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DailyPracticeLimitService {

    public static final int FREE_DAILY_LIMIT = 10;
    static final String KEY_PREFIX = "practice_limit:daily:";
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    StringRedisTemplate stringRedisTemplate;
    SubscriptionService subscriptionService;

    private String getTodayKey(Long userId) {
        return KEY_PREFIX + userId + ":" + LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * Lấy số từ người dùng đã xuất trong ngày hôm nay.
     */
    public int getWordsUsedToday(Long userId) {
        if (userId == null) return 0;
        try {
            String val = stringRedisTemplate.opsForValue().get(getTodayKey(userId));
            return val != null ? Integer.parseInt(val) : 0;
        } catch (Exception e) {
            log.warn("Could not read practice limit from Redis for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }

    /**
     * Lấy số từ còn lại có thể xuất trong ngày hôm nay.
     * PRO: Integer.MAX_VALUE (không giới hạn).
     * FREE: max(0, 10 - wordsUsedToday).
     */
    public int getRemainingWordsToday(Long userId) {
        if (userId == null || subscriptionService.isUserPro(userId)) {
            return Integer.MAX_VALUE;
        }
        int used = getWordsUsedToday(userId);
        return Math.max(0, FREE_DAILY_LIMIT - used);
    }

    /**
     * Kiểm tra xem người dùng có đủ hạn mức để xuất số lượng requestedCount từ không.
     * Ném AppException nếu vượt quá hạn mức ngày của gói Miễn phí.
     */
    public void checkLimit(Long userId, int requestedCount) {
        if (userId == null || subscriptionService.isUserPro(userId)) {
            return;
        }
        int remaining = getRemainingWordsToday(userId);
        if (remaining <= 0) {
            throw new AppException(ErrorCode.PREMIUM_REQUIRED);
        }
        if (requestedCount > remaining) {
            throw new AppException(ErrorCode.PREMIUM_REQUIRED);
        }
    }

    /**
     * Ghi nhận số lượng từ đã xuất trong ngày hôm nay.
     */
    public void recordUsage(Long userId, int count) {
        if (userId == null || count <= 0 || subscriptionService.isUserPro(userId)) {
            return;
        }
        try {
            String key = getTodayKey(userId);
            Long newCount = stringRedisTemplate.opsForValue().increment(key, count);
            // Giữ key trong 48 giờ để tự động giải phóng bộ nhớ
            stringRedisTemplate.expire(key, Duration.ofDays(2));
            log.info("[PRACTICE-LIMIT] User id={} exported {} words, total used today={}/{}",
                    userId, count, newCount, FREE_DAILY_LIMIT);
        } catch (Exception e) {
            log.warn("Could not record practice limit in Redis for user {}: {}", userId, e.getMessage());
        }
    }
}
