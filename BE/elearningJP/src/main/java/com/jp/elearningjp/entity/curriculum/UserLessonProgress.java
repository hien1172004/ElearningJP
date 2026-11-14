package com.jp.elearningjp.entity.curriculum;


import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_lesson_progress")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLessonProgress extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProgressStatus status = ProgressStatus.NOT_STARTED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "best_score", precision = 5, scale = 2)
    private BigDecimal bestScore;

    /** Số mục đã xem theo loại phần (KANJI/VOCABULARY/GRAMMAR) — V67. */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_progress", nullable = false, columnDefinition = "jsonb")
    private Map<LessonSectionType, Integer> sectionProgress = new HashMap<>();

    /** % hoàn thành cả bài, chỉ dùng hiển thị — V67. */
    @Builder.Default
    @Column(name = "progress_percent", nullable = false)
    private Integer progressPercent = 0;

}
