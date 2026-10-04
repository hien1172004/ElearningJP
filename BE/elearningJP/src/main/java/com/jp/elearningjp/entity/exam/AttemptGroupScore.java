package com.jp.elearningjp.entity.exam;

import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;


@Entity
@Table(name = "attempt_group_scores")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptGroupScore extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "score_group_id", nullable = false)
    private ExamScoreGroup scoreGroup;

    @Column(name = "raw_correct_count", nullable = false)
    private Integer rawCorrectCount;

    @Column(name = "raw_total_count", nullable = false)
    private Integer rawTotalCount;

    @Column(name = "scaled_score", nullable = false)
    private Integer scaledScore;

    @Column(name = "is_group_passed", nullable = false)
    private boolean groupPassed;

}
