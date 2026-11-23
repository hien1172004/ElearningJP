package com.jp.elearningjp.service.redis;

import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> ops;

    @InjectMocks RateLimitService service;

    @SuppressWarnings("unchecked")
    private void redisCountReturns(Long n) {
        when(redis.execute(any(RedisScript.class), eq(List.of("rl:k")), eq("60000"))).thenReturn(n);
    }

    @Test
    void check_withinLimit_passes() {
        redisCountReturns(5L);
        assertThatCode(() -> service.check("k", 5, Duration.ofMinutes(1))).doesNotThrowAnyException();
    }

    @Test
    void check_overLimit_throwsTooManyRequests() {
        redisCountReturns(6L);
        assertThatThrownBy(() -> service.check("k", 5, Duration.ofMinutes(1)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisDown_failsOpen() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("down"));
        assertThatCode(() -> service.check("k", 1, Duration.ofMinutes(1))).doesNotThrowAnyException();

        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));
        assertThat(service.isOverLimit("k", 1)).isFalse();
    }

    @Test
    void isOverLimit_readsWithoutIncrementing() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("rl:k")).thenReturn("5");
        assertThat(service.isOverLimit("k", 5)).isTrue();
        assertThat(service.isOverLimit("k", 6)).isFalse();
    }
}
