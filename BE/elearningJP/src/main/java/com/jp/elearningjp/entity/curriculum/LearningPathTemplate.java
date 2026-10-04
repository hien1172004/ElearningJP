package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "learning_path_templates")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningPathTemplate extends SoftDeletableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "jlpt_level", nullable = false, length = 2)
    private JlptLevel jlptLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "daily_vocab_quota", nullable = false)
    private Integer dailyVocabQuota;

    @Column(name = "daily_kanji_quota", nullable = false)
    private Integer dailyKanjiQuota;

    @Column(name = "daily_grammar_quota", nullable = false)
    private Integer dailyGrammarQuota;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

}
