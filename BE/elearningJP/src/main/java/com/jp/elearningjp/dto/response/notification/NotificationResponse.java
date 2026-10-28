package com.jp.elearningjp.dto.response.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.NotificationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Một thông báo trả về FE (REST snapshot + payload STOMP).
 *
 * <p>Field giữ tên {@code dataJson} cho khớp entity/cột {@code data_json}; FE map
 * sang {@code data} khi dùng. Cùng một object được dùng cho cả REST và WebSocket
 * để FE chỉ cần một type duy nhất.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {

    Long id;
    String title;
    String body;
    NotificationType type;
    JsonNode dataJson;
    boolean read;
    Instant createdAt;
}
