package com.jp.elearningjp.dto.response.subscription;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscriptionResponse {
    private String planId;
    private String planName;
    private String status;
    private Boolean isPro;
    private Instant startAt;
    private Instant endAt;
}

