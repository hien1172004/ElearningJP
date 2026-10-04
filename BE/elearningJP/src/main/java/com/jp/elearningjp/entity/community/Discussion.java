package com.jp.elearningjp.entity.community;

import com.jp.elearningjp.entity.curriculum.Lesson;
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

@Entity
@Table(name = "discussions")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discussion extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ContentStatus status = ContentStatus.VISIBLE;

    /** do trigger của DB cập nhật, chỉ đọc */
    @Column(name = "like_count", nullable = false, insertable = false, updatable = false)
    private Integer likeCount;

    /** do trigger của DB cập nhật, chỉ đọc */
    @Column(name = "reply_count", nullable = false, insertable = false, updatable = false)
    private Integer replyCount;


}
