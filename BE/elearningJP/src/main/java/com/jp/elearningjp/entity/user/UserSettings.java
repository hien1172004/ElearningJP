package com.jp.elearningjp.entity.user;

import com.jp.elearningjp.shared.persitence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.UpdateTimestamp;

/** Ánh xạ bảng {@code user_settings}. */
@Entity
@Table(name = "user_settings")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime = LocalTime.of(20, 0);

    @Builder.Default
    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;

    @Builder.Default
    @Column(name = "notify_srs_due", nullable = false)
    private boolean notifySrsDue = true;

    @Builder.Default
    @Column(name = "notify_streak_warning", nullable = false)
    private boolean notifyStreakWarning = true;

    @Builder.Default
    @Column(name = "notify_task_reminder", nullable = false)
    private boolean notifyTaskReminder = true;

    @Builder.Default
    @Column(name = "notify_exam_result", nullable = false)
    private boolean notifyExamResult = true;

    @Builder.Default
    @Column(name = "notify_community", nullable = false)
    private boolean notifyCommunity = true;
}
