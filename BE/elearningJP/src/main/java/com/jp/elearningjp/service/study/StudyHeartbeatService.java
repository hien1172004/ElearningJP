package com.jp.elearningjp.service.study;

import com.jp.elearningjp.dto.request.study.StudyHeartbeatRequest;
import com.jp.elearningjp.dto.response.study.StudyHeartbeatResponse;
import com.jp.elearningjp.entity.gamification.UserDailyStats;
import com.jp.elearningjp.service.gamification.DailyStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;

/**
 * Ghi nhận thời gian học từ FE.
 *
 * <p>Cơ chế chống trùng:</p>
 * <ul>
 *   <li>Redis key {@code study:hb:{userId}} lưu sessionId tab "leader" hiện tại,
 *       TTL 90s (gấp 3 lần interval heartbeat 30s).</li>
 *   <li>Khi nhận heartbeat, so sánh sessionId client gửi với leader hiện tại trong Redis.
 *       <ul>
 *           <li>Nếu khớp → ghi nhận, refresh TTL.</li>
 *           <li>Nếu khác → gọi {@code SETNX} giành quyền; nếu SETNX fail → tab khác đang giữ, bỏ qua.</li>
 *           <li>Nếu key đã expired → gọi {@code SETNX} luôn, trở thành leader mới.</li>
 *       </ul>
 *   </li>
 *   <li>Heartbeat không cộng XP, không ghi study_events (chỉ cập nhật daily_stats).</li>
 * </ul>
 *
 * <p>Nếu Redis fail → fallback cho ghi nhận bình thường (best effort).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyHeartbeatService {

    /** TTL cho Redis leader-lock (giây). Gấp 3 lần interval FE. */
    private static final int LEADER_TTL_SECONDS = 90;

    /** Cap study_seconds/ngày ở mức 16h (an toàn). */
    private static final int MAX_SECONDS_PER_DAY = 16 * 3600;

    private final DailyStatsService dailyStatsService;

    /** Múi giờ nghiệp vụ — xem {@link com.jp.elearningjp.config.TimeConfig}. */
    private final Clock clock;

    @Qualifier("redisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;

    public StudyHeartbeatResponse record(Long userId, StudyHeartbeatRequest req) {
        int seconds = Math.max(1, Math.min(60, req.getSeconds() != null ? req.getSeconds() : 30));
        String sessionId = req.getSessionId();
        String leaderKey = "study:hb:" + userId;

        boolean isLeader = tryAcquireLeader(leaderKey, sessionId);
        if (!isLeader) {
            return rejected("Một tab khác đang giữ quyền đếm thời gian");
        }

        LocalDate today = LocalDate.now(clock);

        // Cap theo ngay
        UserDailyStats todayStats = dailyStatsService.getOrNull(userId, today);
        int alreadyToday = todayStats != null && todayStats.getStudySeconds() != null ? todayStats.getStudySeconds() : 0;
        int remaining = Math.max(0, MAX_SECONDS_PER_DAY - alreadyToday);
        int toAdd = Math.min(seconds, remaining);

        if (toAdd > 0) {
            dailyStatsService.addStudySeconds(userId, today, toAdd, 0);
        }

        return StudyHeartbeatResponse.builder()
                .accepted(true)
                .studySecondsToday(alreadyToday + toAdd)
                .statDate(today)
                .build();
    }

    /**
     * Trả về true nếu session hiện tại nên ghi nhận:
     * - Key Redis null (expired): SETNX mới, OK.
     * - Key trùng sessionId: refresh TTL, OK.
     * - Key khác sessionId: SETNX cũng fail (đã có owner khác), reject.
     */
    private boolean tryAcquireLeader(String key, String sessionId) {
        try {
            Object existing = redisTemplate.opsForValue().get(key);
            if (existing == null) {
                Boolean ok = redisTemplate.opsForValue().setIfAbsent(
                        key, sessionId, Duration.ofSeconds(LEADER_TTL_SECONDS));
                return Boolean.TRUE.equals(ok);
            }
            if (sessionId.equals(String.valueOf(existing))) {
                redisTemplate.expire(key, Duration.ofSeconds(LEADER_TTL_SECONDS));
                return true;
            }
            return false;
        } catch (Exception ex) {
            log.warn("[StudyHeartbeat] redis leader check fail, fallback accept: {}", ex.getMessage());
            return true;
        }
    }

    private StudyHeartbeatResponse rejected(String reason) {
        return StudyHeartbeatResponse.builder()
                .accepted(false)
                .reason(reason)
                .statDate(LocalDate.now(clock))
                .build();
    }
}