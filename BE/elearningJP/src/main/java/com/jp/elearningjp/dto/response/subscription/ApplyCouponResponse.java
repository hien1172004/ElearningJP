package com.jp.elearningjp.dto.response.subscription;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyCouponResponse {
    private Boolean isValid;
    private String code;
    private Integer discountAmountVnd;
    private Integer finalAmountVnd;
    private String message;
}

