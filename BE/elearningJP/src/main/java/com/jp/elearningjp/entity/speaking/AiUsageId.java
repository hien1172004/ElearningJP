package com.jp.elearningjp.entity.speaking;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Composite Primary Key cho bảng ai_usage (user_id + usage_date).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class AiUsageId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;
}
