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
@Table(name = "reading_passages")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingPassage extends SoftDeletableEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "jlpt_level", nullable = false, length = 2)
    private JlptLevel jlptLevel;

    @Column(name = "content_jp", nullable = false, columnDefinition = "text")
    private String contentJp;

    @Column(name = "translation_vi", columnDefinition = "text")
    private String translationVi;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vocab_notes_json", columnDefinition = "jsonb")
    private JsonNode vocabNotesJson;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
