package com.jp.elearningjp.shared.event.comment;

import com.jp.elearningjp.shared.enums.CommentTargetType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Phát ra khi có người like hoặc dislike bình luận / reply.
 *
 * <p>Chỉ chứa dữ liệu <b>đã tách khỏi entity</b> (id + nội dung rút gọn): listener chạy
 * {@code AFTER_COMMIT} — lúc đó persistence context đã đóng, truy cập lazy field của
 * entity sẽ ném {@code LazyInitializationException}.</p>
 */
@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentReactionEvent {

    /** Chủ comment / reply — người sẽ nhận thông báo. */
    Long recipientUserId;

    /** Người like / dislike — dùng để bỏ qua trường hợp tự tác động vào nội dung của chính mình. */
    Long actorUserId;

    /** Tên người tác động (đã resolve sẵn) — dùng trong nội dung thông báo. */
    String actorName;

    /**
     * {@code true} = like, {@code false} = dislike.
     */
    boolean like;

    /**
     * Phạm vi tác động: COMMENT hoặc REPLY.
     */
    ReactionScope scope;

    /** Id của comment (cả 2 scope đều có, để điều hướng). */
    Long commentId;

    /** Id của reply — chỉ có khi {@code scope == REPLY}. */
    Long replyId;

    CommentTargetType targetType;
    Long targetId;

    public enum ReactionScope {
        COMMENT,
        REPLY
    }
}
