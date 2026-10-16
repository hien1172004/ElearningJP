package com.jp.elearningjp.entity.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Quản lý các nghĩa chi tiết của từ vựng (hỗ trợ từ đa nghĩa).
 */
@Entity
@Table(name = "vocabulary_senses")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularySense extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocab_id", nullable = false)
    private Vocabulary vocabulary;

    @Builder.Default
    @Column(name = "sense_no", nullable = false)
    private Short senseNo = (short) 1;

    @Column(name = "meaning_vi", nullable = false, columnDefinition = "text")
    private String meaningVi;

    @Column(name = "meaning_en", columnDefinition = "text")
    private String meaningEn;

    @Column(name = "part_of_speech", length = 50)
    private String partOfSpeech;

    @Column(name = "usage_notes", columnDefinition = "text")
    private String usageNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private JsonNode tags;

    @Builder.Default
    @OneToMany(mappedBy = "sense", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<VocabularyExample> examples = new ArrayList<>();
}