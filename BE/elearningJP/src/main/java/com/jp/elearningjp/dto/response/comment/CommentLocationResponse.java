package com.jp.elearningjp.dto.response.comment;

import com.jp.elearningjp.shared.enums.CommentTargetType;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Vị trí của 1 bình luận (và reply, nếu có) — FE dùng để dựng link mở thẳng tới
 * bình luận khi người dùng bấm thông báo.
 *
 * <p>Tính lúc bấm (không lưu sẵn trong thông báo) nên luôn đúng với dữ liệu hiện tại:
 * slug/từ khóa đổi, bình luận mới chen lên trước… đều không làm link sai.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentLocationResponse {

    Long commentId;

    /** Id reply cần mở; {@code null} nếu không yêu cầu hoặc reply không còn hiển thị. */
    Long replyId;

    CommentTargetType targetType;
    Long targetId;

    /** Id khóa học chứa bài — chỉ có khi {@code targetType == LESSON}. */
    Long courseId;

    /**
     * Từ khóa để mở trang tra cứu: từ vựng ({@code word}), chữ Hán ({@code character}),
     * ngữ pháp ({@code title}). {@code null} với LESSON.
     */
    String keyword;

    /**
     * Thứ tự (bắt đầu từ 0) của bình luận trong danh sách của target, sắp mới nhất trước —
     * FE tự tính trang theo page size của mình: {@code position / size + 1}.
     */
    long position;
}
