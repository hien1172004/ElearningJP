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
 * Liên kết giữa từ vựng và từng chữ Hán cấu thành (phục vụ phân tích chữ Hán kiểu Mazii).
 */
@Entity
@Table(name = "vocabulary_characters")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularyCharacter extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocab_id", nullable = false)
    private Vocabulary vocabulary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false)
    private WritingCharacter character;

    @Builder.Default
    @Column(name = "position", nullable = false)
    private Integer position = 1;

    @Builder.Default
    @Column(name = "is_ateji", nullable = false)
    private boolean ateji = false;
}