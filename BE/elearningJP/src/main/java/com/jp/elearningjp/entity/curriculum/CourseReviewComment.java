package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.persitence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

/** Nhận xét của ADMIN trong 1 lượt duyệt, gắn với cả khóa / 1 bài / 1 mục của bài. */
@Entity
@Table(name = "course_review_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReviewComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_request_id", nullable = false)
    private CourseReviewRequest reviewRequest;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /** null = nhận xét chung cho cả khóa. */
    @Column(name = "lesson_id")
    private Long lessonId;

    /** null = cả bài. */
    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private boolean resolved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
