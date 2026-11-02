package com.jp.elearningjp.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Đẩy thông báo tới các phiên STOMP đang kết nối <b>với instance này</b>.
 *
 * <p>{@code convertAndSendToUser(userId.toString(), "/queue/notifications", payload)}
 * được Spring dịch thành {@code /queue/notifications-user{sessionId}} rồi mới đẩy vào
 * simple broker — nên broker phải bật {@code /queue} (xem {@code WebSocketConfig}).
 * Client subscribe {@code /user/queue/notifications}.</p>
 *
 * <p>Simple broker nằm trong bộ nhớ nên chỉ thấy phiên của chính instance. Để tới được
 * người dùng kết nối ở instance khác, {@link NotificationPublisher} phát qua Redis và mỗi
 * instance gọi lớp này cho phiên của mình ({@link NotificationRedisSubscriber}).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationLocalDelivery {

    static final String DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    /** Lỗi broker bị nuốt tại đây: thông báo vẫn nằm trong DB, lần load REST sau vẫn thấy. */
    public void deliver(Long userId, Object payload) {
        if (userId == null || payload == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), DESTINATION, payload);
            log.debug("[Notification] Delivered to local sessions of userId={}", userId);
        } catch (Exception ex) {
            log.warn("[Notification] STOMP push failed for userId={}: {}", userId, ex.getMessage());
        }
    }
}
