package com.jp.elearningjp.controller.notification;

import com.jp.elearningjp.dto.response.notification.NotificationListResponse;
import com.jp.elearningjp.dto.response.notification.NotificationResponse;
import com.jp.elearningjp.dto.response.notification.UnreadCountResponse;
import com.jp.elearningjp.service.notification.NotificationService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST snapshot cho module thông báo.
 *
 * <p>WebSocket chỉ dùng để <b>đẩy</b> bản ghi mới; các thao tác đọc/đánh dấu vẫn đi
 * REST để giữ tính idempotent và có thể retry an toàn:</p>
 * <ul>
 *   <li>{@code GET  /api/v1/notifications} — phân trang</li>
 *   <li>{@code GET  /api/v1/notifications/unread-count} — badge</li>
 *   <li>{@code GET  /api/v1/notifications/recent} — dropdown nhanh</li>
 *   <li>{@code POST /api/v1/notifications/{id}/read}</li>
 *   <li>{@code POST /api/v1/notifications/read-all}</li>
 * </ul>
 */
@Tag(name = "Notifications", description = "Thông báo trong ứng dụng (chuông báo + real-time STOMP)")
@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Notification.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    NotificationService notificationService;
    CurrentUserService currentUserService;

    @Operation(summary = "Danh sách thông báo (phân trang, mới nhất trước)")
    @GetMapping
    public ApiResponse<NotificationListResponse> getList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Long userId = currentUserService.getCurrentUserId();
        return ApiResponse.<NotificationListResponse>builder()
                .success(true)
                .data(notificationService.getList(userId, page, size))
                .build();
    }

    @Operation(summary = "Số thông báo chưa đọc (badge trên chuông)")
    @GetMapping(ApiPaths.Notification.UNREAD_COUNT)
    public ApiResponse<UnreadCountResponse> getUnreadCount() {
        Long userId = currentUserService.getCurrentUserId();
        return ApiResponse.<UnreadCountResponse>builder()
                .success(true)
                .data(UnreadCountResponse.builder()
                        .count(notificationService.countUnread(userId))
                        .build())
                .build();
    }

    @Operation(summary = "20 thông báo CHƯA ĐỌC mới nhất (cho dropdown)")
    @GetMapping(ApiPaths.Notification.RECENT)
    public ApiResponse<List<NotificationResponse>> getRecent() {
        Long userId = currentUserService.getCurrentUserId();
        return ApiResponse.<List<NotificationResponse>>builder()
                .success(true)
                .data(notificationService.getRecent(userId))
                .build();
    }

    @Operation(summary = "Đánh dấu 1 thông báo đã đọc (idempotent)")
    @PostMapping(ApiPaths.Notification.READ)
    public ApiResponse<Void> markAsRead(@PathVariable("id") Long id) {
        Long userId = currentUserService.getCurrentUserId();
        notificationService.markAsRead(userId, id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã đánh dấu đã đọc")
                .build();
    }

    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc")
    @PostMapping(ApiPaths.Notification.READ_ALL)
    public ApiResponse<Void> markAllAsRead() {
        Long userId = currentUserService.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã đánh dấu tất cả là đã đọc")
                .build();
    }
}
