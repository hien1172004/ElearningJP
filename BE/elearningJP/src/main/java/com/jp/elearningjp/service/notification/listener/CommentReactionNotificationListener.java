package com.jp.elearningjp.service.notification.listener;

import com.jp.elearningjp.repository.gamification.NotificationRepository;
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
 * Sinh thông báo {@code COMMUNITY_LIKE} khi có người like bình luận hoặc reply.
 *
 * <p><b>Không thông báo dislike</b> (quyết định sản phẩm): báo "ai đó không thích bình luận
 * của bạn" dễ gây khó chịu. Dislike vẫn được ghi và đếm, chỉ không gửi thông báo.</p>
 *
 * <p><b>Chống spam:</b> mỗi người like chỉ tạo tối đa 1 thông báo cho 1 comment/reply —
 * bỏ like rồi like lại không tạo thông báo mới.</p>
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
    NotificationRepository notificationRepository;
    UserSettingsRepository userSettingsRepository;
    ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentReaction(CommentReactionEvent event) {
        Long recipientId = event.getRecipientUserId();

        // Không tự nhắn mình (đã lọc ở service, nhưng check lại cho an toàn). Không báo dislike.
        if (recipientId == null || recipientId.equals(event.getActorUserId()) || !event.isLike()) {
            return;
        }

        try {
            if (isCommunityNotificationDisabled(recipientId)) {
                return;
            }
            Long replyKey = event.getReplyId() != null ? event.getReplyId() : -1L;
            if (notificationRepository.existsCommunityLike(
                    recipientId, event.getActorUserId(), event.getCommentId(), replyKey)) {
                return; // đã báo lượt like này rồi (like → bỏ like → like lại)
            }

            ObjectNode data = objectMapper.createObjectNode();
            data.put("commentId", event.getCommentId());
            data.put("actorUserId", event.getActorUserId());
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
                    NotificationType.COMMUNITY_LIKE,
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
        return reply ? "Ai đó đã thích trả lời của bạn" : "Ai đó đã thích bình luận của bạn";
    }

    private String buildBody(CommentReactionEvent event) {
        String name = event.getActorName();
        if (name == null || name.isBlank()) {
            return "Bình luận của bạn vừa nhận được 1 lượt thích.";
        }
        return name + " đã thích " + scopeNoun(event) + " của bạn.";
    }

    private String scopeNoun(CommentReactionEvent event) {
        return event.getScope() == CommentReactionEvent.ReactionScope.REPLY ? "trả lời" : "bình luận";
    }
}
