package com.jp.elearningjp.dto.response.notification;

import com.jp.elearningjp.shared.enums.BroadcastAudience;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/** Một lần gửi thông báo hàng loạt (lịch sử gửi). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BroadcastResponse {
    Long id;
    String title;
    String body;
    BroadcastAudience audience;
    String role;
    Long courseId;
    String courseTitle;
    String link;
    int recipientCount;
    Long senderId;
    String senderName;
    Instant createdAt;
}
