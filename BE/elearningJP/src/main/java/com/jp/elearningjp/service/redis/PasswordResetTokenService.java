package com.jp.elearningjp.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "pwd_reset:";
    private static final long EXPIRY_MINUTES = 15; // token hết hạn sau 15 phút

    /**
     * Tạo token mới và lưu vào Redis
     * Key:   "pwd_reset:<token>"
     * Value: email của user
     */
    public String createToken(String email) {
        // Xóa token cũ nếu có (tránh user spam request)
        // Lưu thêm reverse mapping: email → token để tìm và xóa
        String oldToken = (String) redisTemplate.opsForValue().get(PREFIX + "email:" + email);
        if (oldToken != null) {
            redisTemplate.delete(PREFIX + oldToken);
            redisTemplate.delete(PREFIX + "email:" + email);
        }

        String token = UUID.randomUUID().toString();

        // Lưu token → email
        redisTemplate.opsForValue().set(
                PREFIX + token,
                email,
                EXPIRY_MINUTES, TimeUnit.MINUTES
        );

        // Lưu email → token (để xóa token cũ khi tạo mới)
        redisTemplate.opsForValue().set(
                PREFIX + "email:" + email,
                token,
                EXPIRY_MINUTES, TimeUnit.MINUTES
        );

        log.info("Password reset token created for email: {}", email);
        return token;
    }

    /**
     * Lấy email từ token — trả null nếu token không tồn tại hoặc đã hết hạn
     */
    public String getEmailByToken(String token) {
        return (String) redisTemplate.opsForValue().get(PREFIX + token);
    }

    /**
     * Xóa token sau khi đã dùng — tránh dùng lại
     */
    public void deleteToken(String token, String email) {
        redisTemplate.delete(PREFIX + token);
        redisTemplate.delete(PREFIX + "email:" + email);
    }

    public long getExpiryMinutes() {
        return EXPIRY_MINUTES;
    }
}