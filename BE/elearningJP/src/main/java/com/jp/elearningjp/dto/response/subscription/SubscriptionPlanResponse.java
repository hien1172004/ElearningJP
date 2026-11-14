package com.jp.elearningjp.dto.response.subscription;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanResponse {
    private String id;
    private String name;
    private Integer priceVnd;
    private Integer originalPriceVnd;
    private Integer durationDays;
    private Integer monthlyTokens;
    private Integer maxPracticeWords;
    private Boolean isActive;
}

