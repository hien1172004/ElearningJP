package com.jp.elearningjp.entity.srs;

import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.SrsCardType;
import com.jp.elearningjp.shared.enums.SrsStatus;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "srs_items", indexes = {
    @Index(name = "idx_srs_items_user_due", columnList = "user_id, next_review_at")
})
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SrsItem extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    private SrsCardType cardType = SrsCardType.STANDARD;

    @Builder.Default
    @Column(name = "repetition_count", nullable = false)
    private Integer repetitionCount = 0;

    @Builder.Default
    @Column(name = "lapse_count", nullable = false)
    private Integer lapseCount = 0;

    @Builder.Default
    @Column(name = "ease_factor", nullable = false, precision = 3, scale = 2)
    private BigDecimal easeFactor = new BigDecimal("2.50");

    @Builder.Default
    @Column(name = "interval_days", nullable = false)
    private Integer intervalDays = 0;

    @Builder.Default
    @Column(name = "next_review_at", nullable = false)
    private Instant nextReviewAt = Instant.now();

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SrsStatus status = SrsStatus.LEARNING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id")
    private Vocabulary vocab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private WritingCharacter character;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grammar_id")
    private GrammarPoint grammar;

}
