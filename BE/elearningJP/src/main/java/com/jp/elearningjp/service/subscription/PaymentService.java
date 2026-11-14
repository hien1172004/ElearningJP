package com.jp.elearningjp.service.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.request.subscription.ApplyCouponRequest;
import com.jp.elearningjp.dto.request.subscription.CreatePaymentOrderRequest;
import com.jp.elearningjp.dto.request.subscription.PayosWebhookRequest;
import com.jp.elearningjp.dto.response.subscription.ApplyCouponResponse;
import com.jp.elearningjp.dto.response.subscription.PaymentOrderResponse;
import com.jp.elearningjp.entity.subscription.Coupon;
import com.jp.elearningjp.entity.subscription.PaymentOrder;
import com.jp.elearningjp.entity.subscription.SubscriptionPlan;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.subscription.CouponRepository;
import com.jp.elearningjp.repository.subscription.PaymentOrderRepository;
import com.jp.elearningjp.repository.subscription.SubscriptionPlanRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.util.PayosChecksumUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentService {

    final PaymentOrderRepository paymentOrderRepository;
    final SubscriptionPlanRepository planRepository;
    final CouponRepository couponRepository;
    final UserRepository userRepository;
    final SubscriptionService subscriptionService;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.payos.client-id:}")
    String payosClientId;

    @Value("${app.payos.api-key:}")
    String payosApiKey;

    @Value("${app.payos.checksum-key:}")
    String payosChecksumKey;

    @Value("${app.payos.return-url:http://localhost:5173/pricing}")
    String payosReturnUrl;

    @Value("${app.payos.cancel-url:http://localhost:5173/pricing}")
    String payosCancelUrl;

    /**
     * Tạo đơn hàng thanh toán VietQR qua PayOS
     */
    @Transactional
    public PaymentOrderResponse createPaymentOrder(Long userId, CreatePaymentOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Gói cước không tồn tại: " + request.getPlanId()));

        if ("FREE".equalsIgnoreCase(plan.getId())) {
            throw new IllegalArgumentException("Không thể tạo đơn thanh toán cho gói Miễn phí");
        }

        int originalAmount = plan.getPriceVnd();
        int discountAmount = 0;
        Coupon appliedCoupon = null;

        // Xử lý mã giảm giá nếu có
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            appliedCoupon = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(request.getCouponCode().trim())
                    .orElse(null);
            if (appliedCoupon != null && appliedCoupon.isValid(originalAmount)) {
                discountAmount = appliedCoupon.calculateDiscount(originalAmount);
            }
        }

        int finalAmount = Math.max(1000, originalAmount - discountAmount);

        // Sinh mã order_code số nguyên duy nhất (VD: currentTimeMillis mod 100 tỷ *
        // 1000 + random 3 số)
        long orderCode = (System.currentTimeMillis() % 100_000_000_000L) * 1000
                + ThreadLocalRandom.current().nextInt(100, 999);

        // Mô tả chuyển khoản (ngắn gọn dưới 25 ký tự, không dấu theo chuẩn PayOS)
        String description = "EJP " + orderCode;
        if (description.length() > 25) {
            description = description.substring(0, 25);
        }

        String checkoutUrl = null;
        String qrCode = null;
        String paymentLinkId = null;

        // Gọi PayOS API nếu đã cấu hình Key
        if (payosClientId != null && !payosClientId.isBlank() && payosApiKey != null && !payosApiKey.isBlank()) {
            try {
                String signature = PayosChecksumUtil.createSignature(
                        finalAmount,
                        payosCancelUrl,
                        description,
                        orderCode,
                        payosReturnUrl,
                        payosChecksumKey);

                Map<String, Object> body = new HashMap<>();
                body.put("orderCode", orderCode);
                body.put("amount", finalAmount);
                body.put("description", description);
                body.put("cancelUrl", payosCancelUrl);
                body.put("returnUrl", payosReturnUrl);
                body.put("signature", signature);

                RestClient restClient = RestClient.create();
                String responseStr = restClient.post()
                        .uri("https://api-merchant.payos.vn/v2/payment-requests")
                        .header("x-client-id", payosClientId)
                        .header("x-api-key", payosApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                JsonNode root = objectMapper.readTree(responseStr);
                if ("00".equals(root.path("code").asText())) {
                    JsonNode data = root.path("data");
                    checkoutUrl = data.path("checkoutUrl").asText(null);
                    qrCode = data.path("qrCode").asText(null);
                    paymentLinkId = data.path("paymentLinkId").asText(null);
                } else {
                    log.warn("PayOS API returned code={}: {}", root.path("code").asText(), root.path("desc").asText());
                }
            } catch (Exception e) {
                log.error("Failed to call PayOS API: {}", e.getMessage());
            }
        }

        // Dự phòng cho môi trường dev local nếu chưa có key PayOS thật
        if (qrCode == null) {
            // Chuỗi VietQR dự phòng
            qrCode = String.format(
                    "https://img.vietqr.io/image/MB-0348888888-compact2.png?amount=%d&addInfo=%s&accountName=ELEARNINGJP",
                    finalAmount, description);
            checkoutUrl = payosReturnUrl + "?orderCode=" + orderCode;
        }

        PaymentOrder order = PaymentOrder.builder()
                .orderCode(orderCode)
                .user(user)
                .plan(plan)
                .coupon(appliedCoupon)
                .originalAmountVnd(originalAmount)
                .discountAmountVnd(discountAmount)
                .amountVnd(finalAmount)
                .status("PENDING")
                .checkoutUrl(checkoutUrl)
                .qrCode(qrCode)
                .payosPaymentLinkId(paymentLinkId)
                .build();

        order = paymentOrderRepository.save(order);
        log.info("Created payment order id={}, orderCode={}, amount={}", order.getId(), orderCode, finalAmount);

        return toOrderResponse(order);
    }

    /**
     * Áp dụng thử mã giảm giá để hiển thị số tiền tiết kiệm được
     */
    @Transactional(readOnly = true)
    public ApplyCouponResponse applyCoupon(ApplyCouponRequest request) {
        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Gói cước không tồn tại"));

        int originalPrice = plan.getPriceVnd();

        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(request.getCode().trim())
                .orElse(null);

        if (coupon == null || !coupon.isValid(originalPrice)) {
            return ApplyCouponResponse.builder()
                    .isValid(false)
                    .code(request.getCode())
                    .discountAmountVnd(0)
                    .finalAmountVnd(originalPrice)
                    .message("Mã giảm giá không hợp lệ hoặc đã hết hạn")
                    .build();
        }

        int discount = coupon.calculateDiscount(originalPrice);
        int finalAmount = Math.max(1000, originalPrice - discount);

        return ApplyCouponResponse.builder()
                .isValid(true)
                .code(coupon.getCode())
                .discountAmountVnd(discount)
                .finalAmountVnd(finalAmount)
                .message("Áp dụng mã giảm giá thành công! Tiết kiệm " + String.format("%,d VNĐ", discount))
                .build();
    }

    /**
     * Xử lý Webhook bắn về từ PayOS khi thanh toán thành công
     */
    @Transactional
    public boolean handlePayosWebhook(PayosWebhookRequest request) {
        if (request == null || request.getData() == null) {
            log.warn("Invalid PayOS webhook request received");
            return false;
        }

        Long orderCode = request.getData().getOrderCode();
        if (orderCode == null)
            return false;

        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            log.warn("Payment order not found for orderCode={}", orderCode);
            return false;
        }

        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            log.info("Order code={} is already marked as PAID", orderCode);
            return true;
        }

        // Kiểm tra chữ ký webhook nếu key đã được cấu hình
        if (payosChecksumKey != null && !payosChecksumKey.isBlank() && request.getSignature() != null) {
            Map<String, Object> dataMap = new HashMap<>();
            var d = request.getData();
            dataMap.put("orderCode", d.getOrderCode());
            dataMap.put("amount", d.getAmount());
            dataMap.put("description", d.getDescription());
            dataMap.put("accountNumber", d.getAccountNumber());
            dataMap.put("reference", d.getReference());
            dataMap.put("transactionDateTime", d.getTransactionDateTime());
            dataMap.put("currency", d.getCurrency());
            dataMap.put("paymentLinkId", d.getPaymentLinkId());
            if (!PayosChecksumUtil.verifyWebhookSignature(dataMap, request.getSignature(), payosChecksumKey)) {
                log.warn("PayOS webhook signature verification failed for orderCode={}", orderCode);
                return false;
            }
        }

        // Kiểm tra số tiền thanh toán không được nhỏ hơn amountVnd của đơn hàng
        if (request.getData().getAmount() != null && request.getData().getAmount() < order.getAmountVnd()) {
            log.warn("Payment amount mismatch for orderCode={}: expected={}, received={}",
                    orderCode, order.getAmountVnd(), request.getData().getAmount());
            return false;
        }

        if ("00".equals(request.getCode()) || Boolean.TRUE.equals(request.getSuccess())) {
            order.setStatus("PAID");
            order.setPaidAt(Instant.now());
            paymentOrderRepository.save(order);

            // Tăng số lượt dùng coupon nếu có
            if (order.getCoupon() != null) {
                Coupon c = order.getCoupon();
                c.setUsedCount(c.getUsedCount() + 1);
                couponRepository.save(c);
            }

            // Kích hoạt gói PRO ngay lập tức cho học viên!
            subscriptionService.activateProSubscription(order.getUser().getId(), order.getPlan().getId());
            log.info("Successfully activated PRO for userId={} via orderCode={}", order.getUser().getId(), orderCode);
            return true;
        }

        return false;
    }

    /**
     * Kiểm tra trạng thái đơn hàng (Polling từ frontend)
     */
    @Transactional(readOnly = true)
    public PaymentOrderResponse getOrderStatus(Long userId, Long orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return toOrderResponse(order);
    }

    private PaymentOrderResponse toOrderResponse(PaymentOrder o) {
        return PaymentOrderResponse.builder()
                .id(o.getId())
                .orderCode(o.getOrderCode())
                .planId(o.getPlan().getId())
                .planName(o.getPlan().getName())
                .originalAmountVnd(o.getOriginalAmountVnd())
                .discountAmountVnd(o.getDiscountAmountVnd())
                .amountVnd(o.getAmountVnd())
                .status(o.getStatus())
                .checkoutUrl(o.getCheckoutUrl())
                .qrCode(o.getQrCode())
                .couponCode(o.getCoupon() != null ? o.getCoupon().getCode() : null)
                .createdAt(o.getCreatedAt())
                .paidAt(o.getPaidAt())
                .build();
    }
}
