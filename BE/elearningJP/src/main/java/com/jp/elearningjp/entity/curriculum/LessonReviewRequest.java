package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import com.jp.elearningjp.shared.persitence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Một lượt gửi 1 bài học lên ADMIN duyệt (V62). Các bài gửi cùng lượt chung {@code batchId}. */
@Entity
@Table(name = "lesson_review_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonReviewRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    /** Người gửi; null nếu tài khoản đã bị xóa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    /** Ghi chú của người gửi cho ADMIN (chung cho cả lượt). */
    @Column(name = "note", length = 1000)
    private String note;

    /** Bản nháp của bài lúc gửi ({@code lessons.draft_revision}). */
    @Column(name = "draft_revision", nullable = false)
    private Long draftRevision;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReviewRequestStatus status = ReviewRequestStatus.PENDING;

    /** ADMIN duyệt / từ chối, hoặc giáo viên rút yêu cầu. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    /** Lý do từ chối / ghi chú khi duyệt. */
    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** ADMIN duyệt kèm "thay đổi lớn" → học viên đã hoàn thành bài được báo (V64). */
    @Column(name = "major_change", nullable = false)
    @Builder.Default
    private Boolean majorChange = false;

    /** Nháp TEACHER được gửi (V65). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id")
    private LessonDraft draft;

    /** Bản chụp JSON ({@code LessonSnapshot}) của nháp lúc gửi — duyệt xuất bản đúng bản này (V65). */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb")
    private String content;

    /** {@code lessons.live_revision} lúc gửi; khác live lúc duyệt → bản gửi đã cũ (V65). */
    @Column(name = "base_live_revision")
    private Long baseLiveRevision;
}
