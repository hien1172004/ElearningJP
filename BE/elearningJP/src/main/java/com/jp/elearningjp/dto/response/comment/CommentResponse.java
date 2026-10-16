package com.jp.elearningjp.dto.response.comment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jp.elearningjp.shared.enums.CommentTargetType;
import com.jp.elearningjp.shared.enums.ContentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Response trả về 1 {@code Comment} (không kèm replies).
 * <p>Reply lồng nhau được trả riêng qua endpoint {@code GET /comments/{id}}
 * hoặc qua {@link CommentDetailResponse}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponse {

    Long id;
    CommentTargetType targetType;
    Long targetId;

    /** Thông tin user viết comment. {@code null} nếu user đã bị xoá. */
    CommentUserInfo user;

    String content;
    ContentStatus status;

    Integer likeCount;
    Integer dislikeCount;
    Integer replyCount;

    /** Cờ cho biết current user đã like / dislike comment này hay chưa. */
    Boolean liked;
    Boolean disliked;

    /** Cờ cho biết current user có phải chủ sở hữu comment (dùng để hiển thị nút sửa / xoá). */
    Boolean owner;

    Instant createdAt;
    Instant updatedAt;

    /**
     * Thông tin tối giản của user — tránh leak email / role ra ngoài.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommentUserInfo {
        Long id;
        String fullName;
        String avatarUrl;
    }
}
