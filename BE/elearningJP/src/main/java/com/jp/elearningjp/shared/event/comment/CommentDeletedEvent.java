package com.jp.elearningjp.shared.event.comment;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Phát ra khi xoá mềm một comment hoặc một nhánh reply — để module thông báo dọn các
 * thông báo cũ trỏ tới nội dung đã xoá (bấm vào sẽ không còn gì để xem).
 *
 * <ul>
 *   <li>Xoá comment: {@code replyIds} rỗng → dọn mọi thông báo của comment đó.</li>
 *   <li>Xoá reply: {@code replyIds} là reply đó cùng toàn bộ reply con cháu vừa bị xoá.</li>
 * </ul>
 */
@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentDeletedEvent {
    Long commentId;
    @Builder.Default
    List<Long> replyIds = List.of();

    public boolean isWholeComment() {
        return replyIds == null || replyIds.isEmpty();
    }
}
