package com.jp.elearningjp.dto.response.comment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Response chi tiết 1 {@code Comment} kèm danh sách reply của nó.
 *
 * <p>Dùng cho {@code GET /api/v1/comments/{id}} hoặc trả về sau khi tạo comment mới.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentDetailResponse {

    CommentResponse comment;

    /** Danh sách reply đã sắp xếp theo thời gian tạo tăng dần. */
    List<CommentReplyResponse> replies;
}
