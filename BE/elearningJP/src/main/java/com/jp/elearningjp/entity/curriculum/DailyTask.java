package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.shared.enums.DailyTaskStatus;
import com.jp.elearningjp.shared.enums.DailyTaskType;
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
import java.time.LocalDate;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "daily_tasks")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyTask extends SoftDeletableEntity {


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learning_path_id", nullable = false)
    private LearningPath learningPath;

    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private DailyTaskType taskType;

    @Column(name = "target_count", nullable = false)
    private Integer targetCount;

    @Builder.Default
    @Column(name = "completed_count", nullable = false)
    private Integer completedCount = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DailyTaskStatus status = DailyTaskStatus.PENDING;

    @Column(name = "rescheduled_from_date")
    private LocalDate rescheduledFromDate;

    @Column(name = "completed_at")
    private Instant completedAt;

}
