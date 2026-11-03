package com.jp.elearningjp.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.request.notification.PushSubscriptionRequest;
import com.jp.elearningjp.entity.notification.PushSubscription;
import com.jp.elearningjp.repository.notification.PushSubscriptionRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ Web Push (VAPID): lưu subscription và gửi thông báo đẩy tới trình duyệt.
 *
 * <p>Đây là kênh bổ sung cho thông báo in-app (REST/STOMP). Lỗi gửi push không được
 * làm hỏng luồng chính nên mỗi subscription được bọc try/catch riêng.</p>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WebPushService {

    PushSubscriptionRepository pushSubscriptionRepository;
    UserRepository userRepository;
    PushService pushService;
    ObjectMapper objectMapper;

    @Transactional
    public void subscribe(Long userId, PushSubscriptionRequest request) {
        if (pushSubscriptionRepository.existsByEndpoint(request.getEndpoint())) {
            return; // endpoint đã tồn tại — không ghi đè.
        }
        PushSubscription sub = PushSubscription.builder()
                .user(userRepository.getReferenceById(userId))
                .endpoint(request.getEndpoint())
                .p256dh(request.getP256dh())
                .auth(request.getAuth())
                .build();
        pushSubscriptionRepository.save(sub);
    }

    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        pushSubscriptionRepository.deleteByEndpointAndUser_Id(endpoint, userId);
    }

    /**
     * Gửi push tới tất cả subscription của 1 user. Lỗi từng subscription bị bỏ qua
     * (vd endpoint hết hạn) — không ném ra ngoài.
     */
    public void sendToUser(Long userId, String title, String body, String url) {
        List<PushSubscription> subs = pushSubscriptionRepository.findByUser_Id(userId);
        if (subs.isEmpty()) {
            return;
        }
        for (PushSubscription sub : subs) {
            try {
                send(sub, title, body, url);
            } catch (Exception e) {
                log.warn("[WebPush] Gửi thất bại cho user {} (endpoint {}): {}",
                        userId, sub.getEndpoint(), e.getMessage());
            }
        }
    }

    private void send(PushSubscription sub, String title, String body, String url) throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("body", body);
        if (url != null) {
            payload.put("url", url);
        }
        byte[] bytes = objectMapper.writeValueAsBytes(payload);
        Notification notification = new Notification(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), bytes);
        pushService.send(notification);
    }
}
