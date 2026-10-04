package com.jp.elearningjp.entity.curriculum;


import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.LearningPathStatus;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "learning_paths")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningPath extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private LearningPathTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_level", nullable = false, length = 2)
    private JlptLevel targetLevel;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LearningPathStatus status = LearningPathStatus.ACTIVE;

    @Builder.Default
    @Column(name = "total_vocab_planned", nullable = false)
    private Integer totalVocabPlanned = 0;

    @Builder.Default
    @Column(name = "total_kanji_planned", nullable = false)
    private Integer totalKanjiPlanned = 0;

    @Builder.Default
    @Column(name = "total_grammar_planned", nullable = false)
    private Integer totalGrammarPlanned = 0;

    @Column(name = "last_recalculated_at")
    private Instant lastRecalculatedAt;


}
