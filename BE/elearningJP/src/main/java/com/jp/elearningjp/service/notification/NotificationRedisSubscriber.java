package com.jp.elearningjp.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Nhận thông báo từ kênh Redis {@link NotificationPublisher#CHANNEL} và giao cho các phiên
 * STOMP đang kết nối với instance này. Mỗi instance đều subscribe, nên người dùng nhận được
 * thông báo dù đang kết nối ở instance nào.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final NotificationLocalDelivery localDelivery;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            NotificationPublisher.PushMessage push = objectMapper.readValue(
                    new String(message.getBody(), StandardCharsets.UTF_8),
                    NotificationPublisher.PushMessage.class);
            localDelivery.deliver(push.userId(), push.payload());
        } catch (Exception ex) {
            log.warn("[Notification] Invalid message on Redis channel: {}", ex.getMessage());
        }
    }
}
