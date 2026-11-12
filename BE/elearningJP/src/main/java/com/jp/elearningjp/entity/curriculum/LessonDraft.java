package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.shared.enums.DraftKind;
import com.jp.elearningjp.shared.enums.DraftStatus;
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

/**
 * Bản nháp của 1 bài học theo phía (V65): cha của cây nội dung (section, quiz, luyện nói).
 *
 * <p>Các cột trạng thái ({@code status}, {@code hasChanges}, {@code draftRevision}, người / lúc sửa) chỉ ghi bằng
 * câu lệnh UPDATE có điều kiện trong {@code LessonDraftRepository} — 2 thao tác chồng nhau (sửa / gửi duyệt)
 * được Postgres xếp hàng theo khóa dòng; vì vậy ở đây chỉ đọc.</p>
 */
@Entity
@Table(name = "lesson_drafts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonDraft extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, updatable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 10, updatable = false)
    private DraftKind kind;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, updatable = false)
    private DraftStatus status = DraftStatus.EDITING;

    /** {@code lessons.live_revision} lúc nháp được tạo từ bản live. */
    @Builder.Default
    @Column(name = "base_live_revision", nullable = false, updatable = false)
    private Long baseLiveRevision = 0L;

    @Builder.Default
    @Column(name = "has_changes", nullable = false, updatable = false)
    private Boolean hasChanges = false;

    @Builder.Default
    @Column(name = "draft_revision", nullable = false, updatable = false)
    private Long draftRevision = 0L;

    @Builder.Default
    @Column(name = "needs_import", nullable = false, updatable = false)
    private Boolean needsImport = false;

    @Column(name = "updated_by_name", insertable = false, updatable = false)
    private String updatedByName;

    @Column(name = "content_updated_at", insertable = false, updatable = false)
    private Instant contentUpdatedAt;
}
