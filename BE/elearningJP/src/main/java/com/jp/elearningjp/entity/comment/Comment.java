package com.jp.elearningjp.entity.comment;

import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.CommentTargetType;
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
 * Bình luận của user gắn với 1 đối tượng bất kỳ
 * (LESSON / VOCABULARY / KANJI / GRAMMAR) thông qua cặp
 * ({@code target_type}, {@code target_id}).
 *
 * <p>Các cột count ({@code like_count}, {@code dislike_count}, {@code reply_count})
 * được cập nhật ở application layer (service) thay vì dùng trigger DB,
 * để giữ logic minh bạch và dễ test.</p>
 */
@Entity
@Table(name = "comments")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends SoftDeletableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private CommentTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

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

    @Builder.Default
    @Column(name = "reply_count", nullable = false)
    private Integer replyCount = 0;
}
