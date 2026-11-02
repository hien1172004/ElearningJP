package com.jp.elearningjp.service.notification.listener;

import com.jp.elearningjp.service.notification.NotificationService;
import com.jp.elearningjp.shared.event.comment.CommentDeletedEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dọn thông báo trỏ tới comment/reply vừa bị xoá mềm.
 *
 * <p>Chạy {@code AFTER_COMMIT}: chỉ dọn khi việc xoá đã thực sự commit. try/catch đặt ở
 * đây (không đặt trong {@code NotificationService}) theo cùng lý do với các listener khác.</p>
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CommentDeletedNotificationListener {

    NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentDeleted(CommentDeletedEvent event) {
        try {
            int removed = event.isWholeComment()
                    ? notificationService.removeForComment(event.getCommentId())
                    : notificationService.removeForReplies(event.getReplyIds());
            log.debug("[Notification] Removed {} notification(s) of deleted comment {}", removed, event.getCommentId());
        } catch (Exception ex) {
            log.warn("[Notification] cleanup failed for deleted comment {}: {}", event.getCommentId(), ex.getMessage());
        }
    }
}
