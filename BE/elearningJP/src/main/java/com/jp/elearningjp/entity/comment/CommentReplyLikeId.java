package com.jp.elearningjp.entity.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key cho bảng {@code comment_reply_likes}: (user_id, reply_id).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentReplyLikeId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reply_id", nullable = false)
    private Long replyId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommentReplyLikeId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(replyId, that.replyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, replyId);
    }
}
