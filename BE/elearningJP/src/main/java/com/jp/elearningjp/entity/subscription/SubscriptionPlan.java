package com.jp.elearningjp.entity.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @Column(name = "id", length = 50)
    private String id; // FREE, PRO_1M, PRO_1Y, PRO_LIFETIME

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price_vnd", nullable = false)
    private Integer priceVnd;

    @Column(name = "original_price_vnd")
    private Integer originalPriceVnd;

    @Column(name = "duration_days")
    private Integer durationDays; // null if FREE or LIFETIME

    @Builder.Default
    @Column(name = "monthly_tokens", nullable = false)
    private Integer monthlyTokens = 0;

    @Builder.Default
    @Column(name = "max_practice_words", nullable = false)
    private Integer maxPracticeWords = 10;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}

