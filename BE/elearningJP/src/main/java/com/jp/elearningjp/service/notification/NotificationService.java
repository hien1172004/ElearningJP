package com.jp.elearningjp.service.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.dto.response.notification.NotificationListResponse;
import com.jp.elearningjp.dto.response.notification.NotificationResponse;
import com.jp.elearningjp.entity.gamification.Notification;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.gamification.NotificationRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.enums.NotificationType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Nghiệp vụ thông báo trong ứng dụng.
 *
 * <p><b>Hai kênh song song:</b></p>
 * <ul>
 *   <li><b>REST</b> — snapshot (list / unread-count / recent) và lệnh idempotent
 *       (mark read / read-all). Đây là nguồn sự thật khi FE mất kết nối.</li>
 *   <li><b>STOMP</b> — đẩy bản ghi mới ngay khi phát sinh, qua
 *       {@link NotificationPublisher}.</li>
 * </ul>
 *
 * <p><b>Nguyên tắc lỗi:</b> thông báo là tính năng phụ, không được làm hỏng luồng
 * chính. Nhưng try/catch phải đặt ở <i>caller</i> (listener/scheduler), KHÔNG đặt
 * trong method {@code @Transactional} — bắt exception bên trong sẽ để transaction ở
 * trạng thái rollback-only và gây {@code UnexpectedRollbackException} lúc commit.</p>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationService {

    NotificationRepository notificationRepository;
    UserRepository userRepository;
    NotificationPublisher notificationPublisher;
    Clock clock;

    // ============================================================
    // CREATE (+ push real-time)
    // ============================================================

    /**
     * Tạo thông báo rồi đẩy qua STOMP <b>sau khi commit</b>.
     *
     * <p><b>Vì sao {@code REQUIRES_NEW}?</b> Listener {@code AFTER_COMMIT} chạy khi
     * transaction gốc đã kết thúc — không còn transaction active để ghi bản ghi mới.
     * Nếu dùng {@code REQUIRED} thì thao tác save chạy ngoài transaction và có thể
     * không được flush.</p>
     *
     * <p><b>Vì sao push ở {@code afterCommit}?</b> Tránh client nhận thông báo cho một
     * bản ghi chưa tồn tại (hoặc đã bị rollback) → badge tăng nhưng mở danh sách
     * không thấy, và lần refresh sau badge lại tụt.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponse create(Long userId,
                                       NotificationType type,
                                       String title,
                                       String body,
                                       JsonNode dataJson) {
        User user = userRepository.getReferenceById(userId);

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .dataJson(dataJson)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationResponse payload = toResponse(saved);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationPublisher.publishToUser(userId, payload);
                }
            });
        } else {
            // Không có transaction (gọi ngoài ngữ cảnh transactional) — push ngay.
            notificationPublisher.publishToUser(userId, payload);
        }

        return payload;
    }

    // ============================================================
    // READ
    // ============================================================

    @Transactional(readOnly = true)
    public NotificationListResponse getList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );
        Page<Notification> result = notificationRepository.findByUserId(userId, pageable);

        List<NotificationResponse> items = result.getContent().stream()
                .map(this::toResponse)
                .toList();

        return NotificationListResponse.builder()
                .items(items)
                .total(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .unreadCount(notificationRepository.countUnread(userId))
                .build();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecent(Long userId) {
        return notificationRepository.findRecentUnread(userId, PageRequest.of(0, 20))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countUnread(userId);
    }

    // ============================================================
    // WRITE (idempotent)
    // ============================================================

    /**
     * Đánh dấu 1 thông báo đã đọc.
     *
     * <p>Load entity thay vì chạy update "im lặng" để phân biệt được hai lỗi khác nhau:
     * không tồn tại (404) và của user khác (403). Đã đọc rồi thì không làm gì
     * (idempotent).</p>
     */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByIdAndDeletedFalse(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (notification.getUser() == null
                || !userId.equals(notification.getUser().getId())) {
            throw new AppException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId, Instant.now(clock));
    }

    // ============================================================
    // MAPPING
    // ============================================================

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .type(n.getType())
                .dataJson(n.getDataJson())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
