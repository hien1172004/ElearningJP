package com.jp.elearningjp.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Kết nối WebSocket phải bị đóng khi token dùng lúc CONNECT hết hạn. */
class WebSocketSessionExpiryEnforcerTest {

    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

    private WebSocketSession session(String id, Instant tokenExpiresAt) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        if (tokenExpiresAt != null) {
            attributes.put(WebSocketSessionExpiryEnforcer.TOKEN_EXPIRES_AT, tokenExpiresAt);
        }
        when(session.getId()).thenReturn(id);
        when(session.getAttributes()).thenReturn(attributes);
        return session;
    }

    @Test
    void closesOnlySessionsWhoseTokenExpired() throws Exception {
        WebSocketSessionExpiryEnforcer enforcer =
                new WebSocketSessionExpiryEnforcer(Clock.fixed(NOW, ZoneOffset.UTC));
        WebSocketHandler handler = enforcer.decorate(mock(WebSocketHandler.class));

        WebSocketSession expired = session("a", NOW.minusSeconds(1));
        WebSocketSession valid = session("b", NOW.plusSeconds(600));
        WebSocketSession notYetConnected = session("c", null); // chưa gửi CONNECT
        handler.afterConnectionEstablished(expired);
        handler.afterConnectionEstablished(valid);
        handler.afterConnectionEstablished(notYetConnected);

        enforcer.closeExpiredSessions();

        verify(expired).close(WebSocketSessionExpiryEnforcer.TOKEN_EXPIRED);
        verify(valid, never()).close(any());
        verify(notYetConnected, never()).close(any());
        assertThat(enforcer.trackedSessionCount()).isEqualTo(2);
    }
}
