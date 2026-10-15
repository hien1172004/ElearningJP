package com.jp.elearningjp.dto.response.comment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jp.elearningjp.shared.enums.ContentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

/**
 * Response trả 1 {@code CommentReply}.
 * <p>Reply lồng nhau được làm phẳng — client tự dựng cây từ {@code parentReplyId}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentReplyResponse {

    Long id;
    Long commentId;
    Long parentReplyId;

    CommentResponse.CommentUserInfo user;

    String content;
    ContentStatus status;

    Integer likeCount;
    Integer dislikeCount;

    Boolean liked;
    Boolean disliked;
    Boolean owner;

    Instant createdAt;
    Instant updatedAt;
}
