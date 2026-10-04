package com.jp.elearningjp.entity.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "grammar_points")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrammarPoint extends SoftDeletableEntity {

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "structure", nullable = false, columnDefinition = "text")
    private String structure;

    @Enumerated(EnumType.STRING)
    @Column(name = "jlpt_level", nullable = false, length = 2)
    private JlptLevel jlptLevel;

    @Column(name = "meaning_vi", nullable = false, length = 255)
    private String meaningVi;

    @Column(name = "usage_notes", columnDefinition = "text")
    private String usageNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "examples_json", columnDefinition = "jsonb")
    private JsonNode examplesJson;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;


}
