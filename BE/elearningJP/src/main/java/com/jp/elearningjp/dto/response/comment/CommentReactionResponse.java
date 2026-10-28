package com.jp.elearningjp.dto.response.comment;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Trả về trạng thái reaction đầy đủ sau khi toggle like / dislike.
 *
 * <p>FE dùng object này làm <b>source of truth</b> trong {@code onSuccess}
 * để cập nhật tag like/dislike ngay, không cần reload trang.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentReactionResponse {

    /** Trạng thái sau toggle: user hiện tại có đang like không. */
    boolean liked;

    /** Trạng thái sau toggle: user hiện tại có đang dislike không. */
    boolean disliked;

    /** Tổng số like hiện tại. */
    int likeCount;

    /** Tổng số dislike hiện tại. */
    int dislikeCount;
}
