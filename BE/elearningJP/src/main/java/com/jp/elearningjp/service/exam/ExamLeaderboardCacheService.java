package com.jp.elearningjp.service.exam;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.response.exam.LeaderboardItemResponse;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Cache bảng xếp hạng thi thử theo (cấp độ, khoảng thời gian) trên Redis.
 *
 * <p>Tính bảng xếp hạng phải tải mọi lượt thi đã nộp của 1 cấp độ rồi gom theo user, nên chỉ tính
 * lại tối đa mỗi {@link #TTL}. Danh sách lưu là phần dùng chung cho mọi người xem — cờ
 * "là tôi" ({@code isCurrentUser}) và {@code myRank} được tính lại cho từng request.</p>
 *
 * <p>Không xóa cache chủ động khi có người nộp bài: bảng xếp hạng chậm tối đa {@link #TTL} là chấp nhận được.
 * Redis lỗi → coi như không có cache (caller tự tính từ DB).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamLeaderboardCacheService {

    static final String PREFIX = "exam-leaderboard:";
    public static final Duration TTL = Duration.ofMinutes(5);

    private static final TypeReference<List<LeaderboardItemResponse>> LIST_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /** @return danh sách đã cache, hoặc null nếu chưa có / Redis lỗi. */
    public List<LeaderboardItemResponse> get(JlptLevel level, String period) {
        try {
            String json = redis.opsForValue().get(key(level, period));
            return json == null ? null : objectMapper.readValue(json, LIST_TYPE);
        } catch (Exception e) {
            log.warn("[ExamLeaderboardCache] đọc cache lỗi, tính từ DB: {}", e.getMessage());
            return null;
        }
    }

    public void put(JlptLevel level, String period, List<LeaderboardItemResponse> rankings) {
        try {
            redis.opsForValue().set(key(level, period), objectMapper.writeValueAsString(rankings), TTL);
        } catch (Exception e) {
            log.warn("[ExamLeaderboardCache] ghi cache lỗi (bỏ qua): {}", e.getMessage());
        }
    }

    private static String key(JlptLevel level, String period) {
        return PREFIX + level + ":" + period;
    }
}
