package com.jp.elearningjp.dto.response.subscription;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTokenWalletResponse {
    private Integer trialBalance;
    private Integer dailyFreeBalance;
    private Integer subscriptionBalance;
    private Integer totalAvailableTokens;
    private LocalDate lastDailyReset;
}

