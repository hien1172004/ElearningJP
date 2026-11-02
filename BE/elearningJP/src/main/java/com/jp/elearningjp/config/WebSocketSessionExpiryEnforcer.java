package com.jp.elearningjp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Đóng kết nối WebSocket khi access token dùng lúc CONNECT đã hết hạn.
 *
 * <p>JWT chỉ được kiểm tra ở frame CONNECT ({@link WebSocketAuthInterceptor}); không có bước
 * này thì một kết nối mở bằng token sắp hết hạn vẫn nhận thông báo vô thời hạn, kể cả khi
 * token đã bị thu hồi/hết hạn. Interceptor ghi thời điểm hết hạn vào attribute của phiên;
 * job dưới đây đóng phiên quá hạn. FE tự kết nối lại sau 5s: token trong storage còn hạn thì
 * vào lại ngay, hết hạn thì CONNECT bị từ chối → FE refresh token rồi thử lại.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionExpiryEnforcer implements WebSocketHandlerDecoratorFactory {

    /** Attribute của phiên WebSocket chứa {@link Instant} hết hạn của token. */
    public static final String TOKEN_EXPIRES_AT = "tokenExpiresAt";

    /** Mã đóng riêng của ứng dụng (dải 4000–4999) để log/FE phân biệt với lỗi mạng. */
    static final CloseStatus TOKEN_EXPIRED = new CloseStatus(4001, "Token expired");

    private final Clock clock;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessions.put(session.getId(), session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                sessions.remove(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }

    /**
     * Quét mỗi phút. Không dùng ShedLock: mỗi instance chỉ giữ và tự đóng phiên của chính nó,
     * nên mọi instance đều phải chạy job này.
     */
    @Scheduled(fixedDelay = 60_000)
    public void closeExpiredSessions() {
        Instant now = clock.instant();
        sessions.values().forEach(session -> {
            Object expiresAt = session.getAttributes().get(TOKEN_EXPIRES_AT);
            if (expiresAt instanceof Instant exp && !now.isBefore(exp)) {
                try {
                    session.close(TOKEN_EXPIRED);
                    log.debug("[WS] Closed session {} because its token expired at {}", session.getId(), exp);
                } catch (Exception ex) {
                    log.debug("[WS] Failed to close expired session {}: {}", session.getId(), ex.getMessage());
                } finally {
                    sessions.remove(session.getId());
                }
            }
        });
    }

    /** Số phiên đang theo dõi — cho test/giám sát. */
    int trackedSessionCount() {
        return sessions.size();
    }
}
