package com.jp.elearningjp.service.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.response.notification.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Đẩy thông báo real-time tới mọi instance backend.
 *
 * <p><b>Vì sao qua Redis?</b> STOMP dùng simple broker trong bộ nhớ: instance A chỉ đẩy được
 * cho phiên kết nối với A. Khi chạy nhiều instance (ShedLock cho job nhắc nhở cũng giả định
 * điều này), người dùng kết nối ở B sẽ không nhận được. Publisher phát lên kênh Redis
 * {@link #CHANNEL}; mọi instance (kể cả instance phát) nhận qua
 * {@link NotificationRedisSubscriber} và giao cho phiên cục bộ của mình.</p>
 *
 * <p><b>Redis lỗi:</b> giao thẳng cho phiên cục bộ để ít nhất chạy đúng như khi chỉ có
 * 1 instance. Không {@code @Transactional}; mọi lỗi bị nuốt để không làm hỏng luồng chính.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    public static final String CHANNEL = "notifications:push";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationLocalDelivery localDelivery;

    /** Gói tin trên kênh Redis. Payload giữ dạng cây JSON để giao nguyên vẹn cho client. */
    public record PushMessage(Long userId, JsonNode payload) {
    }

    public void publishToUser(Long userId, NotificationResponse payload) {
        if (userId == null || payload == null) {
            return;
        }
        try {
            String message = objectMapper.writeValueAsString(
                    new PushMessage(userId, objectMapper.valueToTree(payload)));
            redisTemplate.convertAndSend(CHANNEL, message);
            log.debug("[Notification] Published id={} for userId={} to Redis", payload.getId(), userId);
        } catch (Exception ex) {
            log.warn("[Notification] Redis publish failed for userId={}, delivering locally only: {}",
                    userId, ex.getMessage());
            localDelivery.deliver(userId, payload);
        }
    }
}
