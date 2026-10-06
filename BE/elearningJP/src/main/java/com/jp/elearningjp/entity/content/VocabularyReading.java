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
 * Quản lý nhiều cách đọc của một từ (Ateji, cách đọc đặc biệt, v.v.).
 */
@Entity
@Table(name = "vocabulary_readings")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularyReading extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocab_id", nullable = false)
    private Vocabulary vocabulary;

    @Column(name = "reading", nullable = false, length = 100)
    private String reading;

    @Builder.Default
    @Column(name = "reading_type", nullable = false, length = 20)
    private String readingType = "PRIMARY";

    @Column(name = "romaji", length = 150)
    private String romaji;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Builder.Default
    @Column(name = "priority", nullable = false)
    private Integer priority = 1;
}