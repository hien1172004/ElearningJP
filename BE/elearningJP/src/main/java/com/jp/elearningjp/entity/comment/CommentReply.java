package com.jp.elearningjp.entity.comment;

import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.ContentStatus;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

/**
 * Reply cho {@link Comment}. Hỗ trợ reply lồng nhau qua {@code parentReply}
 * (self-reference). Một reply chỉ thuộc về 1 comment gốc.
 */
@Entity
@Table(name = "comment_replies")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentReply extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reply_id")
    private CommentReply parentReply;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ContentStatus status = ContentStatus.VISIBLE;

    @Builder.Default
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Builder.Default
    @Column(name = "dislike_count", nullable = false)
    private Integer dislikeCount = 0;
}
