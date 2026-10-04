package com.jp.elearningjp.entity.content;

import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Ánh xạ bảng {@code character_strokes}. */
@Entity
@Table(name = "character_strokes")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterStroke extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false)
    private WritingCharacter character;

    @Column(name = "stroke_number", nullable = false)
    private Integer strokeNumber;

    @Column(name = "svg_path_data", nullable = false, columnDefinition = "text")
    private String svgPathData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sample_points_json", columnDefinition = "jsonb", nullable = false)
    private List<List<Double>> samplePointsJson;

}
