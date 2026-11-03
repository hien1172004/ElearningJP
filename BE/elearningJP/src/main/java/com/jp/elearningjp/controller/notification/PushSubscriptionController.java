package com.jp.elearningjp.controller.notification;

import com.jp.elearningjp.dto.request.notification.PushSubscriptionRequest;
import com.jp.elearningjp.service.notification.WebPushService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/push")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Push Notification (Web Push)", description = "Đăng ký / hủy nhận thông báo đẩy trình duyệt")
public class PushSubscriptionController {

    WebPushService webPushService;
    CurrentUserService currentUserService;

    @Operation(summary = "Đăng ký nhận thông báo đẩy", description = "Service Worker của FE gửi subscription (endpoint, p256dh, auth) lên để đăng ký nhận push.")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/subscribe")
    public ApiResponse<Void> subscribe(@Valid @RequestBody PushSubscriptionRequest request) {
        webPushService.subscribe(currentUserService.getCurrentUserId(), request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @Operation(summary = "Hủy đăng ký nhận thông báo đẩy")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/unsubscribe")
    public ApiResponse<Void> unsubscribe(@RequestParam String endpoint) {
        webPushService.unsubscribe(currentUserService.getCurrentUserId(), endpoint);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
