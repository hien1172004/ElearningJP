package com.jp.elearningjp.entity.community;

import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.ContentStatus;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "discussion_replies")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscussionReply extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discussion_id", nullable = false)
    private Discussion discussion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reply_id")
    private DiscussionReply parentReply;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ContentStatus status = ContentStatus.VISIBLE;

    /** do trigger của DB cập nhật, chỉ đọc */
    @Column(name = "like_count", nullable = false, insertable = false, updatable = false)
    private Integer likeCount;

    @Builder.Default
    @Column(name = "is_teacher_endorsed", nullable = false)
    private boolean teacherEndorsed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endorsed_by")
    private User endorsedBy;


}
