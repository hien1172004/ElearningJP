package com.jp.elearningjp.entity.exam;


import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.AttemptStatus;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "exam_attempts")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ExamAttempt extends SoftDeletableEntity {


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Builder.Default
    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "total_raw_score", precision = 6, scale = 2)
    private BigDecimal totalRawScore;

    @Column(name = "total_scaled_score")
    private Integer totalScaledScore;

    @Column(name = "is_passed")
    private Boolean passed;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;


}
