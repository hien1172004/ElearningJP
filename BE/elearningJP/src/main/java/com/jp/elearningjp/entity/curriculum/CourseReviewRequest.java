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

/** Một lượt giáo viên gửi khóa học lên ADMIN duyệt xuất bản. */
@Entity
@Table(name = "course_review_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReviewRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** Người gửi; null nếu tài khoản đã bị xóa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    /** Ghi chú của người gửi cho ADMIN. */
    @Column(name = "note", length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReviewRequestStatus status = ReviewRequestStatus.PENDING;

    /** ADMIN duyệt / từ chối, hoặc người rút yêu cầu. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    /** Lý do từ chối / ghi chú khi duyệt. */
    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
