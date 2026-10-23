package com.jp.elearningjp.repository.config;

import com.jp.elearningjp.entity.config.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho bang {@code app_config}.
 *
 * <p>Co 2 cach doc config:
 * <ul>
 *   <li>{@link #findByKey(String)} — doc 1 key (cache local nếu cần)</li>
 *   <li>{@link #findAllConfigs()} — doc tat ca (dùng làm local cache)</li>
 * </ul></p>
 */
@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, String> {

    Optional<AppConfig> findById(String key);

    /** Doc tat ca config (dùng để build local cache). */
    List<AppConfig> findAll();

    /** Doc gia tri Integer cua 1 key, tra Optional. */
    @Query("SELECT CAST(c.value AS integer) FROM AppConfig c WHERE c.key = :key")
    Optional<Integer> findIntValue(String key);

    /** Doc gia tri Integer cua 1 key, tra default neu khong ton tai. */
    default int getIntValue(String key, int defaultValue) {
        return findIntValue(key).orElse(defaultValue);
    }
}
