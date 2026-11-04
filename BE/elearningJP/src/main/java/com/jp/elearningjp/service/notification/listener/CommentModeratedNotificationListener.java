package com.jp.elearningjp.service.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jp.elearningjp.service.notification.NotificationService;
import com.jp.elearningjp.shared.enums.NotificationType;
import com.jp.elearningjp.shared.event.comment.CommentModeratedEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gửi thông báo {@code COMMENT_MODERATED} cho người viết khi bình luận / trả lời của họ bị
 * người kiểm duyệt ẩn hoặc xóa.
 *
 * <p>Đây là thông báo hệ thống nên KHÔNG phụ thuộc cài đặt "thông báo cộng đồng".
 * {@code data_json} cố ý không chứa {@code commentId}/{@code replyId}: listener dọn thông báo
 * khi xóa bình luận ({@code CommentDeletedNotificationListener}) xóa mọi thông báo trỏ tới
 * bình luận đó, sẽ xóa luôn thông báo này nếu có khóa đó.</p>
 *
 * <p>Chạy AFTER_COMMIT + try/catch: lỗi gửi thông báo không làm hỏng thao tác kiểm duyệt.</p>
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CommentModeratedNotificationListener {

    static final int MAX_PREVIEW_LENGTH = 80;

    NotificationService notificationService;
    ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentModerated(CommentModeratedEvent event) {
        Long recipientId = event.getRecipientUserId();
        // Người viết đã bị xóa tài khoản, hoặc tự kiểm duyệt nội dung của mình → không báo
        if (recipientId == null || recipientId.equals(event.getModeratorUserId())) {
            return;
        }
        try {
            ObjectNode data = objectMapper.createObjectNode();
            data.put("action", event.getAction().name());
            data.put("scope", event.getScope().name());
            data.put("targetType", event.getTargetType() != null ? event.getTargetType().name() : null);
            data.put("targetId", event.getTargetId());

            notificationService.create(
                    recipientId,
                    NotificationType.COMMENT_MODERATED,
                    buildTitle(event),
                    buildBody(event),
                    data
            );
        } catch (Exception ex) {
            log.warn("[Notification] comment moderated notify failed -> user {}: {}", recipientId, ex.getMessage());
        }
    }

    static String buildTitle(CommentModeratedEvent event) {
        String what = event.getScope() == CommentModeratedEvent.Scope.REPLY ? "Trả lời" : "Bình luận";
        String verb = event.getAction() == CommentModeratedEvent.Action.DELETED ? "đã bị xóa" : "đã bị ẩn";
        return what + " của bạn " + verb + " do vi phạm quy định cộng đồng";
    }

    static String buildBody(CommentModeratedEvent event) {
        String preview = event.getContentPreview() == null ? "" : event.getContentPreview().trim();
        if (preview.length() > MAX_PREVIEW_LENGTH) {
            preview = preview.substring(0, MAX_PREVIEW_LENGTH) + "…";
        }
        StringBuilder sb = new StringBuilder("\"").append(preview).append("\"");
        String note = event.getNote() == null ? "" : event.getNote().trim();
        if (!note.isEmpty()) {
            sb.append(" — Lý do: ").append(note);
        }
        return sb.toString();
    }
}
