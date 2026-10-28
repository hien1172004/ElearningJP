package com.jp.elearningjp.service.notification;

import com.jp.elearningjp.dto.response.notification.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Đẩy thông báo real-time tới client qua STOMP.
 *
 * <p>{@code convertAndSendToUser(userId.toString(), "/queue/notifications", payload)}
 * được Spring dịch thành {@code /queue/notifications-user{sessionId}} rồi mới đẩy vào
 * simple broker — nên broker phải bật {@code /queue} (xem {@code WebSocketConfig}).
 * Client subscribe {@code /user/queue/notifications}.</p>
 *
 * <p><b>Không {@code @Transactional}:</b> đây là I/O ngoài DB, và mọi lỗi broker phải
 * bị nuốt tại đây để không làm hỏng luồng nghiệp vụ chính (user offline / broker chết
 * thì thông báo vẫn nằm trong DB, lần load REST sau vẫn thấy).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private static final String DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public void publishToUser(Long userId, NotificationResponse payload) {
        if (userId == null || payload == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), DESTINATION, payload);
            log.debug("[Notification] Pushed id={} to userId={}", payload.getId(), userId);
        } catch (Exception ex) {
            log.warn("[Notification] STOMP push failed for userId={}: {}", userId, ex.getMessage());
        }
    }
}
