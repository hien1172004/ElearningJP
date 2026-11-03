package com.jp.elearningjp.dto.request.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** Payload đăng ký nhận thông báo đẩy (do Service Worker của FE gửi lên). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PushSubscriptionRequest {

    @NotBlank(message = "endpoint không được để trống")
    String endpoint;

    @NotBlank(message = "p256dh không được để trống")
    String p256dh;

    @NotBlank(message = "auth không được để trống")
    String auth;
}
