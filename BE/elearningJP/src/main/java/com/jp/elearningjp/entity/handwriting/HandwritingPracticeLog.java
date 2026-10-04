package com.jp.elearningjp.entity.handwriting;

import com.fasterxml.jackson.databind.JsonNode;

import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "handwriting_practice_logs")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HandwritingPracticeLog extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false)
    private WritingCharacter character;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "drawn_strokes_json", columnDefinition = "jsonb", nullable = false)
    private JsonNode drawnStrokesJson;

    @Column(name = "canvas_image_url", length = 500)
    private String canvasImageUrl;

    @Column(name = "stroke_count_drawn", nullable = false)
    private Integer strokeCountDrawn;

    @Column(name = "stroke_order_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal strokeOrderScore;

    @Column(name = "shape_similarity_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal shapeSimilarityScore;

    @Column(name = "total_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "is_passed", nullable = false)
    private boolean passed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recognized_topk_json", columnDefinition = "jsonb", nullable = false)
    private JsonNode recognizedTopkJson;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "feedback_text", length = 255)
    private String feedbackText;

}
