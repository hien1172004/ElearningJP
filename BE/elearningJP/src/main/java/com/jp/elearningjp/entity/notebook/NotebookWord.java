package com.jp.elearningjp.entity.notebook;

import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.shared.enums.MemoryLevel;
import com.jp.elearningjp.shared.enums.NotebookItemType;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "notebook_words", indexes = {
    @Index(name = "idx_notebook_words_notebook_id", columnList = "notebook_id"),
    @Index(name = "idx_notebook_words_srs_due", columnList = "notebook_id, next_review_at"),
    @Index(name = "idx_notebook_words_vocab_id", columnList = "vocabulary_id")
})
@SQLRestriction("deleted = false")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotebookWord extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;

    /** Loại kiến thức của thẻ (Từ vựng / Chữ Hán / Ngữ pháp) — V59. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 10)
    private NotebookItemType itemType = NotebookItemType.VOCAB;

    /** Từ vựng gốc trong từ điển (chỉ với itemType = VOCAB). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id")
    private Vocabulary vocabulary;

    /** Chữ Hán gốc (chỉ với itemType = KANJI). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private WritingCharacter character;

    /** Mẫu ngữ pháp gốc (chỉ với itemType = GRAMMAR). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grammar_id")
    private GrammarPoint grammar;

    /** Bài học mà thẻ được lưu từ đó (null nếu thêm từ Từ điển / nhập tay). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_lesson_id")
    private Lesson sourceLesson;

    @Column(name = "word", nullable = false, length = 100)
    private String word;

    @Column(name = "reading", length = 100)
    private String reading;

    @Column(name = "meaning", nullable = false, columnDefinition = "text")
    private String meaning;

    @Column(name = "part_of_speech", length = 50)
    private String partOfSpeech;

    @Column(name = "user_note", columnDefinition = "text")
    private String userNote;

    @Column(name = "sino_vietnamese", length = 100)
    private String sinoVietnamese;

    @Column(name = "example_sentence", columnDefinition = "text")
    private String exampleSentence;

    @Column(name = "example_meaning", columnDefinition = "text")
    private String exampleMeaning;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "jlpt_level", length = 2)
    private String jlptLevel;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    // SRS (SM-2) fields
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "memory_level", nullable = false, length = 20)
    private MemoryLevel memoryLevel = MemoryLevel.NOT_LEARNED;

    @Builder.Default
    @Column(name = "interval_days", nullable = false)
    private Integer intervalDays = 0;

    @Builder.Default
    @Column(name = "repetition_count", nullable = false)
    private Integer repetitionCount = 0;

    @Builder.Default
    @Column(name = "lapse_count", nullable = false)
    private Integer lapseCount = 0;

    @Builder.Default
    @Column(name = "ease_factor", nullable = false, precision = 3, scale = 2)
    private BigDecimal easeFactor = new BigDecimal("2.50");

    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;
}

