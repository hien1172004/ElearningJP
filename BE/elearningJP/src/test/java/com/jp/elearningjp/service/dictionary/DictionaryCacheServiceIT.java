package com.jp.elearningjp.service.dictionary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.response.dictionary.VocabularySummaryResponse;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.response.PageResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Cache từ điển trên Redis thật: JSON của DTO thật đọc lại được, SCAN xóa đúng nhóm từ vựng. Cần Docker. */
@Testcontainers
class DictionaryCacheServiceIT {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate redis;

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private DictionaryCacheService service;

    @BeforeAll
    static void connect() {
        factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
    }

    @AfterAll
    static void close() {
        factory.destroy();
    }

    @BeforeEach
    void setUp() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
        service = new DictionaryCacheService(redis, objectMapper);
    }

    @Test
    void pageOfImmutableList_roundTripsThroughRedis() {
        String key = DictionaryCacheService.searchKey("vocab", "taberu", JlptLevel.N5, 0, 20);
        // List.of(...) như Stream.toList() trong code thật — loại list làm hỏng JSON kèm kiểu
        PageResponse<VocabularySummaryResponse> page = PageResponse.<VocabularySummaryResponse>builder()
                .items(List.of(VocabularySummaryResponse.builder()
                        .id(1L).word("食べる").hiragana("たべる").romaji("taberu").jlptLevel(JlptLevel.N5).build()))
                .page(0).size(20).totalElements(1).totalPages(1).build();
        TypeReference<PageResponse<VocabularySummaryResponse>> type = new TypeReference<>() {};

        service.getOrLoad(key, type, Duration.ofHours(1), () -> page);
        PageResponse<VocabularySummaryResponse> cached =
                service.getOrLoad(key, type, Duration.ofHours(1), () -> { throw new AssertionError("không được tra DB"); });

        assertThat(cached).usingRecursiveComparison().isEqualTo(page);
        assertThat(redis.getExpire(key)).isBetween(3500L, 3600L);
    }

    @Test
    void evictVocabulary_removesOnlyVocabularyGroups() {
        for (String k : List.of("dict:vocab:N5:0:20:a", "dict:vocab:ALL:1:20:b", "dict:unified:ALL:-:-:a",
                "dict:quick:ALL:-:-:食べる", "dict:kanji:N5:0:20:a", "dict:grammar:ALL:0:20:a", "dict:kana:HIRAGANA",
                "leaderboard:weekly:v3:x")) {
            redis.opsForValue().set(k, "1");
        }

        service.evictVocabularyAfterCommit(); // không có transaction → xóa ngay

        assertThat(redis.keys("*")).containsExactlyInAnyOrder(
                "dict:kanji:N5:0:20:a", "dict:grammar:ALL:0:20:a", "dict:kana:HIRAGANA", "leaderboard:weekly:v3:x");
    }
}
