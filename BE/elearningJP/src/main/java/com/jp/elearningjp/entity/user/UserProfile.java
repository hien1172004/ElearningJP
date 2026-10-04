package com.jp.elearningjp.entity.user;


import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.LearnerLevel;
import com.jp.elearningjp.shared.persitence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.UpdateTimestamp;

/** Ánh xạ bảng {@code user_profiles}. */
@Entity
@Table(name = "user_profiles")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserProfile extends BaseEntity {


    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "current_level", nullable = false, length = 10)
    private LearnerLevel currentLevel = LearnerLevel.BEGINNER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "target_level", nullable = false, length = 2)
    private JlptLevel targetLevel = JlptLevel.N5;

    @Column(name = "target_exam_date")
    private LocalDate targetExamDate;

    @Builder.Default
    @Column(name = "daily_study_time_minutes", nullable = false)
    private Integer dailyStudyTimeMinutes = 30;

    @Builder.Default
    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

}
