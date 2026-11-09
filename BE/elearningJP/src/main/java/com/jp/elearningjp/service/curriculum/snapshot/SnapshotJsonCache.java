package com.jp.elearningjp.service.curriculum.snapshot;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cache trong bộ nhớ cho JSON snapshot (khóa học, bài học, đề thi).
 *
 * <ul>
 *   <li>Snapshot <b>bất biến</b> sau khi tạo và id phiên bản không dùng lại → không cần xóa cache khi ghi.</li>
 *   <li>Giữ dạng cây JSON đã parse; mỗi lần đọc dựng object mới từ cây → service ghép tiến độ riêng
 *       của từng học viên vào object trả về mà không làm bẩn cache.</li>
 *   <li>Giới hạn số mục (LRU), cấu hình {@code app.snapshot.cache-size} (mặc định 100);
 *       0 = tắt cache. Mỗi instance có cache riêng — không cần đồng bộ vì dữ liệu không đổi.</li>
 * </ul>
 */
@Component
@Slf4j
public class SnapshotJsonCache {

    private final ObjectMapper objectMapper;
    private final int maxEntries;
    private final Map<String, JsonNode> cache;

    public SnapshotJsonCache(ObjectMapper objectMapper, @Value("${app.snapshot.cache-size:100}") int maxEntries) {
        this.objectMapper = objectMapper;
        this.maxEntries = Math.max(0, maxEntries);
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, JsonNode> eldest) {
                return size() > SnapshotJsonCache.this.maxEntries;
            }
        });
    }

    /**
     * Đọc snapshot theo {@code key}; chưa có trong cache thì gọi {@code loader} (đọc JSON từ DB).
     * Không có dữ liệu → {@code Optional.empty()} (không cache kết quả rỗng).
     */
    public <T> Optional<T> get(String key, Class<T> type, Supplier<Optional<String>> loader) {
        JsonNode node = cache.get(key);
        if (node == null) {
            Optional<String> json = loader.get();
            if (json.isEmpty()) {
                return Optional.empty();
            }
            node = readTree(json.get());
            if (maxEntries > 0) {
                cache.put(key, node);
            }
        }
        return Optional.of(toValue(node, type));
    }

    int size() {
        return cache.size();
    }

    private JsonNode readTree(String json) {
        try {
            // Giữ số thập phân dạng BigDecimal (điểm câu hỏi 1.00...) thay vì double
            return objectMapper.reader()
                    .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                    .with(JsonNodeFactory.withExactBigDecimals(true))
                    .readTree(json);
        } catch (IOException e) {
            log.error("[SNAPSHOT] Cannot parse snapshot JSON", e);
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }
    }

    private <T> T toValue(JsonNode node, Class<T> type) {
        try {
            // Bỏ qua trường lạ: snapshot cũ vẫn đọc được khi DTO thêm / bớt trường
            return objectMapper.readerFor(type)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(node);
        } catch (IOException e) {
            log.error("[SNAPSHOT] Cannot map snapshot to {}", type.getSimpleName(), e);
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }
    }
}
