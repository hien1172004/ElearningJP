package com.jp.elearningjp.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "blacklist:";

    /**
     * Thêm token vào blacklist.
     * TTL = thời gian còn lại đến khi token hết hạn → Redis tự xóa, không cần @Scheduled
     */
    public void blacklist(String jwtId, Date expiryTime) {
        long ttlMillis = expiryTime.getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            redisTemplate.opsForValue()
                    .set(PREFIX + jwtId, "1", ttlMillis, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Kiểm tra token có trong blacklist không
     */
    public boolean isBlacklisted(String jwtId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + jwtId));
    }
}