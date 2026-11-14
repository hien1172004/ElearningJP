package com.jp.elearningjp.dto.request.subscription;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayosWebhookRequest {
    private String code;
    private String desc;
    private Boolean success;
    private PayosWebhookData data;
    private String signature;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PayosWebhookData {
        private Long orderCode;
        private Integer amount;
        private String description;
        private String accountNumber;
        private String reference;
        private String transactionDateTime;
        private String currency;
        private String paymentLinkId;
    }
}

