package com.jp.elearningjp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket + STOMP cho module thông báo.
 *
 * <p><b>Transport:</b> raw WebSocket (không SockJS) — FE dùng
 * {@code brokerURL: ws://host/ws}. Endpoint handshake nằm ở root {@code /ws}
 * (không thuộc {@code /api/v1}).</p>
 *
 * <p><b>Vì sao broker phải bật {@code /queue} chứ không phải {@code /user}?</b><br>
 * {@code convertAndSendToUser(userId, "/queue/notifications", payload)} được Spring
 * dịch thành đích {@code /queue/notifications-user{sessionId}} rồi mới đẩy vào simple
 * broker. Nếu chỉ bật {@code /user} thì không có kênh nào nhận message → client im lặng
 * không nhận được gì. {@code /user} chỉ là <i>user destination prefix</i> mà client
 * dùng khi subscribe ({@code /user/queue/notifications}).</p>
 *
 * <p><b>Xác thực:</b> handshake HTTP không mang được header {@code Authorization}
 * (giới hạn của trình duyệt) nên {@code /ws} được permitAll ở SecurityConfig; xác thực
 * thật sự diễn ra ở frame STOMP CONNECT qua {@link WebSocketAuthInterceptor}.</p>
 *
 * <p>Không dùng {@code @MessageMapping}: server chủ động push qua
 * {@code SimpMessagingTemplate}.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Trùng đúng danh sách trong {@link SecurityConfig#corsConfigurationSource()}.
     * WebSocket không đi qua CORS HTTP nên phải khai báo lại ở đây.
     */
    private static final String[] ALLOWED_ORIGIN_PATTERNS = {
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://*.up.railway.app",
            "https://*.onrender.com"
    };

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // KHÔNG gọi .withSockJS() — FE dùng raw WebSocket.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
