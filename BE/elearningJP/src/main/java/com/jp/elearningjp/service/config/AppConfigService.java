package com.jp.elearningjp.service.config;

import com.jp.elearningjp.entity.config.AppConfig;
import com.jp.elearningjp.repository.config.AppConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service doc cau hinh tu bang {@code app_config} voi local in-memory cache.
 *
 * <p>Config duoc nap luc khoi dong va refresh định kỳ (5 phút).
 * Nhờ vậy {@link #getIntValue} khong đánh đổi DB mỗi lần tra từ.</p>
 *
 * <p>Fail-safe: nếu reload lỗi, giữ cache cũ.</p>
 */
@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class AppConfigService {

    private final AppConfigRepository appConfigRepository;

    /** Cache key → value (String). */
    private final AtomicReference<Map<String, String>> cache = new AtomicReference<>(Map.of());

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Tra gia tri Integer cua 1 key, tra default neu khong ton tai. */
    public int getIntValue(String key, int defaultValue) {
        String raw = cache.get().get(key);
        if (raw == null) return defaultValue;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Tra gia tri Integer cua 1 key, tra Optional. */
    public java.util.Optional<Integer> findIntValue(String key) {
        String raw = cache.get().get(key);
        return AppConfig.parseInt(raw);
    }

    // -------------------------------------------------------------------------
    // Cache lifecycle
    // -------------------------------------------------------------------------

    /** Nap cau hinh luc khoi dong. */
    @PostConstruct
    public void init() {
        reload();
    }

    /** Reload cau hinh tu DB moi 5 phut. */
    @Scheduled(fixedRate = 300_000) // 5 phut
    public void reload() {
        try {
            List<AppConfig> all = appConfigRepository.findAll();
            Map<String, String> fresh = new ConcurrentHashMap<>();
            for (AppConfig cfg : all) {
                fresh.put(cfg.getKey(), cfg.getValue());
            }
            cache.set(Map.copyOf(fresh));
            log.debug("[AppConfig] reloaded {} keys", fresh.size());
        } catch (Exception ex) {
            log.warn("[AppConfig] reload failed, keeping old cache: {}", ex.getMessage());
        }
    }
}
