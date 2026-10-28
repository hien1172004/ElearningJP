package com.jp.elearningjp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Xác thực frame STOMP {@code CONNECT} bằng JWT.
 *
 * <p>Frame CONNECT không đi qua chuỗi filter HTTP của Spring Security, nên
 * {@code SecurityContextHolder} luôn rỗng trong thread WebSocket. Interceptor này:</p>
 * <ol>
 *   <li>Đọc native header {@code Authorization: Bearer <token>} của frame CONNECT.</li>
 *   <li>Verify + decode bằng {@link CustomJwtDecoder} (đã bao gồm check chữ ký, hạn, blacklist).</li>
 *   <li>Gán principal = <b>chuỗi userId</b> để khớp
 *       {@code convertAndSendToUser(userId.toString(), ...)} phía publisher.</li>
 * </ol>
 *
 * <p><b>Thất bại phải ném exception, không {@code return false}:</b> trả {@code false}
 * chỉ lặng lẽ bỏ frame, client treo ở trạng thái "đang kết nối"; ném
 * {@link MessageDeliveryException} khiến server gửi {@code ERROR} frame để client
 * biết mà dừng/refresh token.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final CustomJwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Chỉ xử lý CONNECT; SUBSCRIBE/SEND/DISCONNECT... cho đi nguyên trạng.
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String header = accessor.getFirstNativeHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            log.debug("[WS] CONNECT rejected: missing Authorization header");
            throw new MessageDeliveryException(message, "Missing Authorization header");
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            Jwt jwt = jwtDecoder.decode(token);
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim == null) {
                throw new MessageDeliveryException(message, "Token has no userId claim");
            }
            String principalName = String.valueOf(userIdClaim);
            accessor.setUser(new UsernamePasswordAuthenticationToken(principalName, null, List.of()));
            log.debug("[WS] CONNECT authenticated for userId={}", principalName);
        } catch (MessageDeliveryException ex) {
            throw ex;
        } catch (Exception ex) {
            log.debug("[WS] CONNECT rejected: invalid token ({})", ex.getMessage());
            throw new MessageDeliveryException(message, "Invalid or expired token");
        }

        return message;
    }
}
