package com.jp.elearningjp.service.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jp.elearningjp.repository.user.UserSettingsRepository;
import com.jp.elearningjp.service.notification.NotificationService;
import com.jp.elearningjp.shared.enums.NotificationType;
import com.jp.elearningjp.shared.event.comment.CommentRepliedEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sinh thông báo {@code COMMUNITY_REPLY} khi có người trả lời bình luận.
 *
 * <p>Chạy {@code AFTER_COMMIT} theo đúng pattern
 * {@code LessonCompletedGamificationListener}: reply chỉ được commit thì mới thông báo,
 * và việc tạo thông báo không làm chậm response của API reply.</p>
 *
 * <p><b>try/catch đặt ở đây, không đặt trong {@code NotificationService}:</b> method
 * service là {@code @Transactional}; bắt exception bên trong sẽ đánh dấu transaction
 * rollback-only và gây {@code UnexpectedRollbackException} lúc commit.</p>
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CommentReplyNotificationListener {

    private static final int MAX_BODY_LENGTH = 120;

    NotificationService notificationService;
    UserSettingsRepository userSettingsRepository;
    ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentReplied(CommentRepliedEvent event) {
        Long recipientId = event.getRecipientUserId();

        // Không tự nhắn mình khi chủ comment trả lời chính bình luận của mình.
        if (recipientId == null || recipientId.equals(event.getReplierUserId())) {
            return;
        }

        try {
            if (isCommunityNotificationDisabled(recipientId)) {
                return;
            }

            ObjectNode data = objectMapper.createObjectNode();
            data.put("commentId", event.getCommentId());
            data.put("replyId", event.getReplyId());
            data.put("targetType", event.getTargetType() != null
                    ? event.getTargetType().name() : null);
            data.put("targetId", event.getTargetId());

            notificationService.create(
                    recipientId,
                    NotificationType.COMMUNITY_REPLY,
                    "Có trả lời mới về bình luận của bạn",
                    buildBody(event),
                    data
            );
        } catch (Exception ex) {
            log.warn("[Notification] community reply failed for comment {} -> user {}: {}",
                    event.getCommentId(), recipientId, ex.getMessage());
        }
    }

    private boolean isCommunityNotificationDisabled(Long userId) {
        return userSettingsRepository.findByUser_Id(userId)
                .map(settings -> !settings.isNotifyCommunity())
                .orElse(false);
    }

    private String buildBody(CommentRepliedEvent event) {
        String content = event.getReplyContent() == null ? "" : event.getReplyContent().trim();
        if (content.length() > MAX_BODY_LENGTH) {
            content = content.substring(0, MAX_BODY_LENGTH) + "…";
        }
        String name = event.getReplierName();
        if (name == null || name.isBlank()) {
            return content;
        }
        return name + ": " + content;
    }
}
