package com.jp.elearningjp.dto.response.subscription;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrderResponse {
    private UUID id;
    private Long orderCode;
    private String planId;
    private String planName;
    private Integer originalAmountVnd;
    private Integer discountAmountVnd;
    private Integer amountVnd;
    private String status; // PENDING, PAID, CANCELLED, EXPIRED
    private String checkoutUrl;
    private String qrCode;
    private String couponCode;
    private Instant createdAt;
    private Instant paidAt;
}

