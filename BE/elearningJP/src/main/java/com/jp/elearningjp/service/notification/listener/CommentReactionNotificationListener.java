package com.jp.elearningjp.service.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jp.elearningjp.repository.user.UserSettingsRepository;
import com.jp.elearningjp.service.notification.NotificationService;
import com.jp.elearningjp.shared.enums.NotificationType;
import com.jp.elearningjp.shared.event.comment.CommentReactionEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sinh thông báo {@code COMMUNITY_LIKE} / {@code COMMUNITY_DISLIKE}
 * khi có người like / dislike bình luận hoặc reply.
 *
 * <p>Chạy {@code AFTER_COMMIT} theo đúng pattern
 * {@code CommentReplyNotificationListener}: like chỉ được commit thì mới thông báo,
 * và việc tạo thông báo không làm chậm response của API toggle.</p>
 *
 * <p>Dùng chung cờ {@code notify_community} với reply — không tách setting riêng.</p>
 *
 * <p><b>try/catch đặt ở đây, không đặt trong {@code NotificationService}:</b> method
 * service là {@code @Transactional}; bắt exception bên trong sẽ đánh dấu transaction
 * rollback-only và gây {@code UnexpectedRollbackException} lúc commit.</p>
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CommentReactionNotificationListener {

    NotificationService notificationService;
    UserSettingsRepository userSettingsRepository;
    ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentReaction(CommentReactionEvent event) {
        Long recipientId = event.getRecipientUserId();

        // Không tự nhắn mình (đã lọc ở service, nhưng check lại cho an toàn).
        if (recipientId == null || recipientId.equals(event.getActorUserId())) {
            return;
        }

        try {
            if (isCommunityNotificationDisabled(recipientId)) {
                return;
            }

            ObjectNode data = objectMapper.createObjectNode();
            data.put("commentId", event.getCommentId());
            if (event.getReplyId() != null) {
                data.put("replyId", event.getReplyId());
            }
            data.put("targetType", event.getTargetType() != null
                    ? event.getTargetType().name() : null);
            data.put("targetId", event.getTargetId());
            data.put("like", event.isLike());
            data.put("scope", event.getScope() != null ? event.getScope().name() : null);

            notificationService.create(
                    recipientId,
                    event.isLike() ? NotificationType.COMMUNITY_LIKE : NotificationType.COMMUNITY_DISLIKE,
                    buildTitle(event),
                    buildBody(event),
                    data
            );
        } catch (Exception ex) {
            log.warn("[Notification] community reaction failed for comment {} -> user {}: {}",
                    event.getCommentId(), recipientId, ex.getMessage());
        }
    }

    private boolean isCommunityNotificationDisabled(Long userId) {
        return userSettingsRepository.findByUser_Id(userId)
                .map(settings -> !settings.isNotifyCommunity())
                .orElse(false);
    }

    private String buildTitle(CommentReactionEvent event) {
        boolean reply = event.getScope() == CommentReactionEvent.ReactionScope.REPLY;
        if (event.isLike()) {
            return reply ? "Ai đó đã thích trả lời của bạn" : "Ai đó đã thích bình luận của bạn";
        }
        return reply ? "Ai đó đã đánh giá trả lời của bạn" : "Ai đó đã đánh giá bình luận của bạn";
    }

    private String buildBody(CommentReactionEvent event) {
        String name = event.getActorName();
        if (name == null || name.isBlank()) {
            return event.isLike() ? "Bình luận của bạn vừa nhận được 1 lượt thích."
                    : "Bình luận của bạn vừa nhận được 1 lượt đánh giá.";
        }
        return event.isLike()
                ? name + " đã thích " + scopeNoun(event) + " của bạn."
                : name + " đã đánh giá " + scopeNoun(event) + " của bạn là không hữu ích.";
    }

    private String scopeNoun(CommentReactionEvent event) {
        return event.getScope() == CommentReactionEvent.ReactionScope.REPLY ? "trả lời" : "bình luận";
    }
}
