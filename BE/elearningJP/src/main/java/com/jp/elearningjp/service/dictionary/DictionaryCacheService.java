package com.jp.elearningjp.service.dictionary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Cache kết quả tra cứu từ điển (danh sách/phân trang) trên Redis, lưu JSON theo kiểu rõ ràng.
 *
 * <p>Dữ liệu từ điển gần như không đổi (nạp bằng migration), còn truy vấn tìm kiếm
 * {@code LIKE '%...%'} phải quét toàn bảng → cache theo (loại, cấp độ, trang, size, từ khóa).</p>
 *
 * <p>Không dùng {@code @Cacheable}: JSON kèm thông tin kiểu của RedisConfig không đọc lại được
 * list bất biến ({@code Stream.toList()}), còn Java serialization bắt mọi DTO phải {@code Serializable}.</p>
 *
 * <p>Redis lỗi → gọi thẳng DB (fail-open).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryCacheService {

    public static final String PREFIX = "dict:";
    public static final Duration SEARCH_TTL = Duration.ofHours(1);
    public static final Duration KANA_TTL = Duration.ofHours(24);
    /** Từ khóa/size vượt ngưỡng → không cache, tránh key rác. */
    public static final int MAX_CACHEABLE_KEYWORD = 50;
    public static final int MAX_CACHEABLE_SIZE = 50;

    /** Các nhóm chứa từ vựng — xóa khi có từ vựng mới. */
    private static final List<String> VOCAB_GROUPS = List.of("vocab", "unified", "quick");

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /**
     * Key cho 1 truy vấn tìm kiếm, hoặc {@code null} nếu không nên cache.
     * Từ khóa đặt cuối để dấu {@code :} trong từ khóa không làm lẫn các phần trước.
     */
    public static String searchKey(String group, String keyword, Object level, Integer page, Integer size) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.length() > MAX_CACHEABLE_KEYWORD || (size != null && size > MAX_CACHEABLE_SIZE)) {
            return null;
        }
        return PREFIX + group + ":" + (level == null ? "ALL" : level) + ":"
                + (page == null ? "-" : page) + ":" + (size == null ? "-" : size) + ":" + kw;
    }

    /** Đọc cache; chưa có (hoặc {@code key == null}, hoặc Redis lỗi) → gọi {@code loader} rồi lưu lại. */
    public <T> T getOrLoad(String key, TypeReference<T> type, Duration ttl, Supplier<T> loader) {
        if (key == null) {
            return loader.get();
        }
        try {
            String json = redis.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, type);
            }
        } catch (Exception e) {
            log.warn("[DictionaryCache] đọc {} lỗi, tra DB: {}", key, e.getMessage());
            return loader.get();
        }
        T value = loader.get();
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("[DictionaryCache] ghi {} lỗi (bỏ qua): {}", key, e.getMessage());
        }
        return value;
    }

    /**
     * Xóa cache các nhóm có chứa từ vựng. Trong transaction → chờ commit xong mới xóa,
     * để request đọc song song không kịp cache lại dữ liệu cũ.
     */
    public void evictVocabularyAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictVocabulary();
                }
            });
        } else {
            evictVocabulary();
        }
    }

    void evictVocabulary() {
        try {
            for (String group : VOCAB_GROUPS) {
                List<String> keys = new ArrayList<>();
                // SCAN thay KEYS: KEYS khóa Redis trong lúc quét
                try (Cursor<String> cursor = redis.scan(
                        ScanOptions.scanOptions().match(PREFIX + group + ":*").count(500).build())) {
                    cursor.forEachRemaining(keys::add);
                }
                if (!keys.isEmpty()) {
                    redis.delete(keys);
                }
            }
        } catch (Exception e) {
            log.warn("[DictionaryCache] xóa cache từ vựng lỗi, chờ TTL tự hết: {}", e.getMessage());
        }
    }
}
