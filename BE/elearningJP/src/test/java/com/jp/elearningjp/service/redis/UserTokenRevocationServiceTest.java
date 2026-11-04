package com.jp.elearningjp.service.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserTokenRevocationServiceTest {

    private ValueOperations<String, String> ops;
    private UserTokenRevocationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        service = new UserTokenRevocationService(redis);
        service.refreshableDuration = 86400;
    }

    @Test
    void revokeAllTokens_storesTimestampWithRefreshableTtl() {
        service.revokeAllTokens(7L);
        verify(ops).set(eq("user-revoked:7"), anyString(), eq(86400L), eq(TimeUnit.SECONDS));
    }

    @Test
    void tokenIssuedBeforeRevocation_isRevoked() {
        when(ops.get("user-revoked:7")).thenReturn("1000");
        assertThat(service.isRevoked(7L, Instant.ofEpochSecond(999))).isTrue();
    }

    /** Tái hiện bug: đăng nhập lại ngay trong cùng giây admin đổi role thì token MỚI bị chặn nhầm. */
    @Test
    void tokenIssuedInSameSecondOrAfterRevocation_isValid() {
        when(ops.get("user-revoked:7")).thenReturn("1000");
        assertThat(service.isRevoked(7L, Instant.ofEpochSecond(1000))).isFalse();
        assertThat(service.isRevoked(7L, Instant.ofEpochSecond(1001))).isFalse();
    }

    @Test
    void noRevocationKey_orMissingClaim_isValid() {
        when(ops.get("user-revoked:7")).thenReturn(null);
        assertThat(service.isRevoked(7L, Instant.now())).isFalse();
        assertThat(service.isRevoked(null, Instant.now())).isFalse();
    }
}
