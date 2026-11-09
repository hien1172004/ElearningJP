package com.jp.elearningjp.service.curriculum.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.response.curriculum.LessonOutlineResponse;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotJsonCacheTest {

    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
    private final AtomicInteger loads = new AtomicInteger();

    private Supplier<Optional<String>> loader(String json) {
        return () -> {
            loads.incrementAndGet();
            return Optional.ofNullable(json);
        };
    }

    private String courseJson() throws Exception {
        return mapper.writeValueAsString(CourseSnapshot.builder()
                .lessons(new ArrayList<>(List.of(LessonOutlineResponse.builder().id(1L).title("Bài 1").build())))
                .build());
    }

    @Test
    void secondRead_servedFromCache_withoutLoadingAgain() throws Exception {
        SnapshotJsonCache cache = new SnapshotJsonCache(mapper, 10);
        String json = courseJson();

        cache.get("course:1", CourseSnapshot.class, loader(json));
        Optional<CourseSnapshot> again = cache.get("course:1", CourseSnapshot.class, loader(json));

        assertThat(again).get().extracting(s -> s.getLessons().get(0).getTitle()).isEqualTo("Bài 1");
        assertThat(loads).hasValue(1);
    }

    @Test
    void eachRead_returnsFreshObject_soPerUserProgressDoesNotLeak() throws Exception {
        SnapshotJsonCache cache = new SnapshotJsonCache(mapper, 10);
        String json = courseJson();

        CourseSnapshot first = cache.get("course:1", CourseSnapshot.class, loader(json)).orElseThrow();
        first.getLessons().get(0).setStatus(ProgressStatus.COMPLETED); // ghép tiến độ của học viên A
        CourseSnapshot second = cache.get("course:1", CourseSnapshot.class, loader(json)).orElseThrow();

        assertThat(second).isNotSameAs(first);
        assertThat(second.getLessons().get(0).getStatus()).isNull();
    }

    @Test
    void missingSnapshot_notCached() {
        SnapshotJsonCache cache = new SnapshotJsonCache(mapper, 10);

        assertThat(cache.get("lesson:1:2", LessonSnapshot.class, loader(null))).isEmpty();
        assertThat(cache.get("lesson:1:2", LessonSnapshot.class, loader(null))).isEmpty();
        assertThat(loads).hasValue(2);
        assertThat(cache.size()).isZero();
    }

    @Test
    void boundedSize_evictsLeastRecentlyUsed() throws Exception {
        SnapshotJsonCache cache = new SnapshotJsonCache(mapper, 2);
        String json = courseJson();

        cache.get("a", CourseSnapshot.class, loader(json));
        cache.get("b", CourseSnapshot.class, loader(json));
        cache.get("a", CourseSnapshot.class, loader(json)); // a vừa dùng → b cũ nhất
        cache.get("c", CourseSnapshot.class, loader(json)); // đẩy b ra
        assertThat(cache.size()).isEqualTo(2);

        loads.set(0);
        cache.get("a", CourseSnapshot.class, loader(json));
        cache.get("b", CourseSnapshot.class, loader(json));
        assertThat(loads).hasValue(1); // chỉ b phải đọc lại
    }

    @Test
    void sizeZero_disablesCache() throws Exception {
        SnapshotJsonCache cache = new SnapshotJsonCache(mapper, 0);
        String json = courseJson();

        cache.get("a", CourseSnapshot.class, loader(json));
        cache.get("a", CourseSnapshot.class, loader(json));

        assertThat(loads).hasValue(2);
    }
}
