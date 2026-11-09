package com.jp.elearningjp.shared.event.comment;

import com.jp.elearningjp.shared.enums.CommentTargetType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Phát ra khi người kiểm duyệt (ADMIN / TEACHER được giao khóa) ẩn hoặc xóa bình luận / trả lời
 * của người khác — để module thông báo báo cho người viết.
 *
 * <p>Mọi field đã resolve sẵn (listener chạy AFTER_COMMIT, không lazy-load được nữa).</p>
 */
@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentModeratedEvent {

    public enum Action { HIDDEN, DELETED }

    public enum Scope { COMMENT, REPLY }

    /** Người viết nội dung bị kiểm duyệt (người nhận thông báo). */
    Long recipientUserId;
    Long moderatorUserId;
    Action action;
    Scope scope;
    /** Trích đoạn nội dung bị ẩn/xóa để người viết nhận ra. */
    String contentPreview;
    /** Ghi chú của người kiểm duyệt (không bắt buộc). */
    String note;
    CommentTargetType targetType;
    Long targetId;
}
