package com.jp.elearningjp.entity.handwriting;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/**
 * Một lần luyện viết Kanji đã chấm điểm (Hanzi-Writer hoặc AI KanjiVG, bằng chuột hoặc bằng tay).
 * Cấu trúc bảng: migration V49.
 */
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

    /** Bài học nơi luyện viết; null nếu luyện ở từ điển / sổ tay. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(name = "character", nullable = false, length = 8)
    private String character;

    @Enumerated(EnumType.STRING)
    @Column(name = "practice_mode", nullable = false, length = 10)
    private HandwritingPracticeMode practiceMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_mode", nullable = false, length = 10)
    private HandwritingInputMode inputMode;

    @Column(name = "total_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "stroke_count_expected")
    private Integer strokeCountExpected;

    @Column(name = "stroke_count_drawn")
    private Integer strokeCountDrawn;

    @Column(name = "total_mistakes")
    private Integer totalMistakes;

    @Column(name = "is_passed", nullable = false)
    private boolean passed;

    /** Chi tiết chấm (stroke_details từ AI…) để xem lại; không bắt buộc. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_json", columnDefinition = "jsonb")
    private JsonNode detailJson;
}
