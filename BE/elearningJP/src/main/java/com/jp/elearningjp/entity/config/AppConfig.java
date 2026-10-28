package com.jp.elearningjp.entity.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Bang cau hinh he thong (key-value global).
 *
 * <p>Su dung cho cac tham so can thay doi khong can deploy:
 * lookup_ceiling, lookup_xp, lesson_xp, min_study_seconds, ...</p>
 *
 * <p>Khong co created_at / deleted / version vi day la bang cau hinh don gian.</p>
 */
@Entity
@Table(name = "app_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfig {

    @Id
    @Column(name = "key", length = 64)
    private String key;

    @Column(name = "value", nullable = false, length = 255)
    private String value;

    @Column(name = "description", length = 255)
    private String description;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Lay gia tri Integer, tra default neu khong ton tai / loi parse. */
    public int getIntValue(int defaultValue) {
        try {
            return Integer.parseInt(this.value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Lay gia tri Integer, tra Optional.empty neu khong ton tai / loi parse. */
    public static java.util.Optional<Integer> parseInt(String raw) {
        if (raw == null || raw.isBlank()) return java.util.Optional.empty();
        try {
            return java.util.Optional.of(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }
}
