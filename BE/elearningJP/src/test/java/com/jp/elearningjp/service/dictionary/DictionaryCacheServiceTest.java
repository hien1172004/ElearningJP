package com.jp.elearningjp.service.dictionary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** getOrLoad: trúng cache không tra DB, trượt thì tra + lưu, Redis lỗi thì vẫn trả kết quả. */
@ExtendWith(MockitoExtension.class)
class DictionaryCacheServiceTest {

    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {};

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> ops;

    private final AtomicInteger dbCalls = new AtomicInteger();

    private DictionaryCacheService service() {
        return new DictionaryCacheService(redis, new ObjectMapper());
    }

    private List<String> loadFromDb() {
        dbCalls.incrementAndGet();
        return List.of("食べる");
    }

    @Test
    void hit_returnsCachedWithoutDb() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("dict:k")).thenReturn("[\"飲む\"]");

        assertThat(service().getOrLoad("dict:k", TYPE, Duration.ofHours(1), this::loadFromDb)).containsExactly("飲む");
        assertThat(dbCalls).hasValue(0);
    }

    @Test
    void miss_loadsFromDbAndStoresWithTtl() {
        when(redis.opsForValue()).thenReturn(ops);

        assertThat(service().getOrLoad("dict:k", TYPE, Duration.ofHours(1), this::loadFromDb)).containsExactly("食べる");
        assertThat(dbCalls).hasValue(1);
        verify(ops).set("dict:k", "[\"食べる\"]", Duration.ofHours(1));
    }

    @Test
    void redisDown_stillAnswersFromDb() {
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(service().getOrLoad("dict:k", TYPE, Duration.ofHours(1), this::loadFromDb)).containsExactly("食べる");
        assertThat(dbCalls).hasValue(1);
    }

    @Test
    void nullKey_bypassesRedis() {
        assertThat(service().getOrLoad(null, TYPE, Duration.ofHours(1), this::loadFromDb)).containsExactly("食べる");
        verifyNoInteractions(redis);
    }

    @Test
    void searchKey_formatsAndSkipsOversizedRequests() {
        assertThat(DictionaryCacheService.searchKey("vocab", "  taberu ", JlptLevel.N5, 0, 20))
                .isEqualTo("dict:vocab:N5:0:20:taberu");
        assertThat(DictionaryCacheService.searchKey("unified", null, null, null, null))
                .isEqualTo("dict:unified:ALL:-:-:");
        assertThat(DictionaryCacheService.searchKey("vocab", "a".repeat(51), null, 0, 20)).isNull();
        assertThat(DictionaryCacheService.searchKey("vocab", "a", null, 0, 51)).isNull();
    }

    @Test
    void evictOutsideTransaction_runsImmediately_andSwallowsRedisErrors() {
        when(redis.scan(any())).thenThrow(new RedisConnectionFailureException("down"));

        service().evictVocabularyAfterCommit(); // không ném lỗi

        verify(redis, never()).delete(anyString());
    }
}
