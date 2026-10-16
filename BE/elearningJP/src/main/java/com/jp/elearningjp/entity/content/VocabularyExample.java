package com.jp.elearningjp.entity.content;

import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

/**
 * Quản lý các câu ví dụ minh họa của từ vựng hoặc của nghĩa cụ thể.
 */
@Entity
@Table(name = "vocabulary_examples")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularyExample extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocab_id", nullable = false)
    private Vocabulary vocabulary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sense_id")
    private VocabularySense sense;

    @Column(name = "example_jp", nullable = false, columnDefinition = "text")
    private String exampleJp;

    @Column(name = "example_vi", nullable = false, columnDefinition = "text")
    private String exampleVi;

    @Column(name = "example_romaji", columnDefinition = "text")
    private String exampleRomaji;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;
}