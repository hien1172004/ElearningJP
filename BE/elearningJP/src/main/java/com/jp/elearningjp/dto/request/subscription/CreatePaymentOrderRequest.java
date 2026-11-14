package com.jp.elearningjp.dto.request.subscription;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentOrderRequest {

    @NotBlank(message = "Vui lòng chọn gói cước cần nâng cấp")
    private String planId; // PRO_1M, PRO_1Y, PRO_LIFETIME

    private String couponCode; // Tùy chọn (ví dụ: ELEARNING2026)
}

