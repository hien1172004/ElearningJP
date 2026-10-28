package com.jp.elearningjp.shared.event.comment;

import com.jp.elearningjp.shared.enums.CommentTargetType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Phát ra khi có reply mới cho một comment.
 *
 * <p>Chỉ chứa dữ liệu <b>đã tách khỏi entity</b> (id + nội dung rút gọn): listener chạy
 * {@code AFTER_COMMIT} — lúc đó persistence context đã đóng, truy cập lazy field của
 * entity sẽ ném {@code LazyInitializationException}.</p>
 */
@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentRepliedEvent {

    /** Chủ comment gốc — người sẽ nhận thông báo. */
    Long recipientUserId;

    /** Người viết reply — dùng để bỏ qua trường hợp tự reply chính mình. */
    Long replierUserId;

    /** Tên người trả lời (đã resolve sẵn) — dùng trong nội dung thông báo. */
    String replierName;

    Long commentId;
    Long replyId;

    CommentTargetType targetType;
    Long targetId;

    /** Nội dung reply gốc; listener tự cắt ngắn trước khi lưu. */
    String replyContent;
}
