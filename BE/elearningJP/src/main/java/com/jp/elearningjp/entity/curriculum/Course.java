package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.CourseReviewStatus;
import com.jp.elearningjp.shared.enums.JlptLevel;
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
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "courses")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course extends SoftDeletableEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "jlpt_level", nullable = false, length = 2)
    private JlptLevel jlptLevel;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "is_published", nullable = false)
    private Boolean published;

    @Column(name = "is_premium", nullable = false)
    @Builder.Default
    private Boolean isPremium = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "archived_at")
    private Instant archivedAt;

    /** Trạng thái bản đang soạn: EDITING | PENDING_REVIEW (chờ ADMIN duyệt, giáo viên bị khóa sửa). */
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    @Builder.Default
    private CourseReviewStatus reviewStatus = CourseReviewStatus.EDITING;

    /** Bản đang soạn có thay đổi chưa được duyệt / xuất bản. */
    @Column(name = "has_unpublished_changes", nullable = false)
    @Builder.Default
    private Boolean hasUnpublishedChanges = false;

    /** Phiên bản xuất bản học viên đang học ({@code course_published_versions.id}); null = chưa có snapshot. */
    @Column(name = "published_version_id")
    private Long publishedVersionId;

    /**
     * Người sửa bản đang soạn gần nhất (id thô, không association — danh sách khóa không phải join users).
     * Ghi qua {@code CourseRepository.markDraftEdited}; null = chưa ai sửa từ khi có cột / tài khoản đã xóa.
     */
    @Column(name = "draft_updated_by")
    private Long draftUpdatedById;

    /** Tên chụp lúc sửa (hiển thị "Đang soạn bởi …"). */
    @Column(name = "draft_updated_by_name", length = 255)
    private String draftUpdatedByName;

    @Column(name = "draft_updated_at")
    private Instant draftUpdatedAt;

}
