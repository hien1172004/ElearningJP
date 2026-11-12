package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.shared.enums.CourseReviewStatus;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

/**
 * Entity {@code Lesson} — bài học thuộc một {@link Course}.
 *
 * <p>V65: nội dung thuộc 2 bản nháp theo phía ({@link LessonDraft}) — không map collection nội dung trên Lesson.</p>
 *
 * <p>SAU KHI REFACTOR (V14): bảng {@code lessons} chỉ còn giữ thông tin khung
 * của bài học (title, thứ tự, archived). Toàn bộ nội dung (Kanji / Vocabulary /
 * Grammar / Quiz) được lưu trong {@link LessonSection} và các bảng con
 * (lesson_kanji_items, lesson_vocabulary_items, lesson_grammar_items,
 * lesson_quizzes).</p>
 *
 * <p>Loại bỏ hoàn toàn các trường cũ: {@code lessonType}, {@code contentMarkdown},
 * {@code durationMinutes}.</p>
 */
@Entity
@Table(name = "lessons")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "archived_at")
    private Instant archivedAt;



    /**
     * Bài có trong phiên bản đang xuất bản (dùng tính tiến độ học viên).
     * Chỉ đọc: ghi bằng câu lệnh khi tạo snapshot ({@code CourseSnapshotService}).
     */
    @Column(name = "in_published_version", insertable = false, updatable = false)
    private Boolean inPublishedVersion;

    /**
     * Tăng mỗi lần nội dung live của bài đổi (V65); nháp dựa trên số cũ hơn là nháp đã cũ.
     * Chỉ đọc: ghi bằng câu lệnh ({@code LessonRepository#bumpLiveRevision}).
     */
    @Column(name = "live_revision", insertable = false, updatable = false)
    private Long liveRevision;

    /**
     * Phía tạo bài khi bài chưa từng lên bản học viên (V66): chỉ phía này thấy bài; null = cả hai phía thấy.
     * Ghi lúc tạo bài; xóa về null bằng câu lệnh khi bài lên bản học viên ({@code LessonRepository#markPublishedLessons}).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "created_kind", length = 10, updatable = false)
    private com.jp.elearningjp.shared.enums.DraftKind createdKind;

    // ---- Duyệt theo bài (V62). CHỈ ĐỌC qua entity: ghi bằng câu lệnh UPDATE có điều kiện trong
    // LessonRepository để 2 thao tác chồng nhau (sửa / gửi duyệt) được DB xếp hàng theo khóa dòng,
    // và để save() của entity (vd: sửa tiêu đề) không ghi đè giá trị cũ đang nằm trong bộ nhớ.

    /** EDITING | PENDING_REVIEW (bài chờ duyệt: chỉ đọc với mọi người, kể cả ADMIN). */
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", insertable = false, updatable = false)
    private CourseReviewStatus reviewStatus;

    /** Bản nháp của bài khác bản live. */
    @Column(name = "has_unpublished_changes", insertable = false, updatable = false)
    private Boolean hasUnpublishedChanges;

    /** Tăng mỗi lần ghi nội dung bài; gửi duyệt phải khớp số này. */
    @Column(name = "draft_revision", insertable = false, updatable = false)
    private Long draftRevision;

    @Column(name = "draft_updated_by_name", insertable = false, updatable = false)
    private String draftUpdatedByName;

    @Column(name = "draft_updated_at", insertable = false, updatable = false)
    private Instant draftUpdatedAt;
}
