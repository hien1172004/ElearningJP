package com.jp.elearningjp.dto.response.notification;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Danh sách thông báo phân trang + số chưa đọc (theo mẫu {@code CommentListResponse}). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationListResponse {

    List<NotificationResponse> items;
    long total;
    int page;
    int size;
    int totalPages;
    long unreadCount;
}
