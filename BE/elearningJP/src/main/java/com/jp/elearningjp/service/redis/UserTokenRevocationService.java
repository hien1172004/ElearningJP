package com.jp.elearningjp.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Thu hồi toàn bộ token đã cấp cho một user (khi admin khóa tài khoản hoặc đổi role).
 *
 * <p>Lưu mốc thời gian (epoch giây) vào Redis: mọi JWT có {@code iat} không sau mốc này đều bị
 * coi là không hợp lệ. TTL = refreshable-duration, vì sau khoảng đó không token cũ nào
 * còn dùng/refresh được nữa nên key tự hết hạn.
 */
@Service
@RequiredArgsConstructor
public class UserTokenRevocationService {

    private static final String PREFIX = "user-revoked:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refreshable-duration:86400}")
    long refreshableDuration;

    public void revokeAllTokens(Long userId) {
        redisTemplate.opsForValue().set(
                PREFIX + userId,
                String.valueOf(Instant.now().getEpochSecond()),
                refreshableDuration,
                TimeUnit.SECONDS);
    }

    /**
     * Token có bị thu hồi không. JWT lưu {@code iat} theo giây nên so sánh {@code <}:
     * token cấp trong cùng giây với lúc thu hồi vẫn hợp lệ, để user đăng nhập lại ngay sau khi
     * admin đổi role không bị chặn nhầm. Khe hở ≤ 1 giây này chấp nhận được vì login/refresh
     * còn kiểm tra trạng thái tài khoản.
     */
    public boolean isRevoked(Long userId, Instant issuedAt) {
        if (userId == null || issuedAt == null) return false;
        String value = redisTemplate.opsForValue().get(PREFIX + userId);
        if (value == null) return false;
        try {
            return issuedAt.getEpochSecond() < Long.parseLong(value);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
