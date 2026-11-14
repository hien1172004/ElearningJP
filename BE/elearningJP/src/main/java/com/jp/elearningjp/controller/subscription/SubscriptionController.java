package com.jp.elearningjp.controller.subscription;

import com.jp.elearningjp.dto.request.subscription.ApplyCouponRequest;
import com.jp.elearningjp.dto.request.subscription.CreatePaymentOrderRequest;
import com.jp.elearningjp.dto.request.subscription.PayosWebhookRequest;
import com.jp.elearningjp.dto.response.subscription.ApplyCouponResponse;
import com.jp.elearningjp.dto.response.subscription.PaymentOrderResponse;
import com.jp.elearningjp.dto.response.subscription.SubscriptionPlanResponse;
import com.jp.elearningjp.dto.response.subscription.UserSubscriptionResponse;
import com.jp.elearningjp.dto.response.subscription.UserTokenWalletResponse;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.service.subscription.PaymentService;
import com.jp.elearningjp.service.subscription.SubscriptionService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Subscription & Payment", description = "Quản lý gói PRO, ví token AI và thanh toán VietQR PayOS")
@RestController
@RequestMapping(ApiPaths.API_V1 + "/subscriptions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SubscriptionController {

    SubscriptionService subscriptionService;
    PaymentService paymentService;
    CurrentUserService currentUserService;

    @Operation(summary = "Lấy danh sách các gói hội viên công khai", security = {})
    @GetMapping("/plans")
    public ApiResponse<List<SubscriptionPlanResponse>> getPublicPlans() {
        return ApiResponse.<List<SubscriptionPlanResponse>>builder()
                .success(true)
                .data(subscriptionService.getPublicPlans())
                .build();
    }

    @Operation(summary = "Lấy thông tin gói hội viên của người dùng hiện tại")
    @GetMapping("/my-subscription")
    public ApiResponse<UserSubscriptionResponse> getMySubscription() {
        User user = currentUserService.getCurrentUser();
        return ApiResponse.<UserSubscriptionResponse>builder()
                .success(true)
                .data(subscriptionService.getMySubscription(user.getId()))
                .build();
    }

    @Operation(summary = "Lấy số dư ví Token AI của người dùng hiện tại")
    @GetMapping("/my-wallet")
    public ApiResponse<UserTokenWalletResponse> getMyWallet() {
        User user = currentUserService.getCurrentUser();
        return ApiResponse.<UserTokenWalletResponse>builder()
                .success(true)
                .data(subscriptionService.getMyWallet(user.getId()))
                .build();
    }

    @Operation(summary = "Kiểm tra và áp dụng mã giảm giá / voucher")
    @PostMapping("/apply-coupon")
    public ApiResponse<ApplyCouponResponse> applyCoupon(@RequestBody @Valid ApplyCouponRequest request) {
        return ApiResponse.<ApplyCouponResponse>builder()
                .success(true)
                .data(paymentService.applyCoupon(request))
                .build();
    }

    @Operation(summary = "Tạo đơn hàng thanh toán VietQR PayOS")
    @PostMapping("/create-order")
    public ApiResponse<PaymentOrderResponse> createPaymentOrder(@RequestBody @Valid CreatePaymentOrderRequest request) {
        User user = currentUserService.getCurrentUser();
        return ApiResponse.<PaymentOrderResponse>builder()
                .success(true)
                .data(paymentService.createPaymentOrder(user.getId(), request))
                .build();
    }

    @Operation(summary = "Kiểm tra trạng thái đơn hàng thanh toán (Polling)")
    @GetMapping("/orders/{orderCode}")
    public ApiResponse<PaymentOrderResponse> getOrderStatus(@PathVariable Long orderCode) {
        User user = currentUserService.getCurrentUser();
        return ApiResponse.<PaymentOrderResponse>builder()
                .success(true)
                .data(paymentService.getOrderStatus(user.getId(), orderCode))
                .build();
    }

    @Operation(summary = "Webhook nhận thông báo thanh toán tự động từ PayOS", security = {})
    @PostMapping("/webhook")
    public Map<String, Object> handlePayosWebhook(@RequestBody PayosWebhookRequest request) {
        log.info("Received PayOS webhook for orderCode={}", request.getData() != null ? request.getData().getOrderCode() : null);
        boolean handled = paymentService.handlePayosWebhook(request);
        return Map.of("success", handled);
    }
}

