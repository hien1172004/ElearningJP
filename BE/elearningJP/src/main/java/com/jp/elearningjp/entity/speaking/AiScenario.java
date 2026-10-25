package com.jp.elearningjp.entity.speaking;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.persitence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity lưu thông tin Kịch bản tình huống nhập vai luyện nói AI.
 */
@Entity
@Table(name = "ai_scenario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiScenario extends BaseEntity {

    @Column(name = "topic_id", nullable = false, length = 50)
    private String topicId; // DINING_SHOPPING, LIVING_IN_JAPAN, FRIENDSHIP_CHATTING, TRAVEL, ASKING_HELP...

    @Enumerated(EnumType.STRING)
    @Column(name = "jlpt_level", nullable = false, length = 10)
    private JlptLevel jlptLevel;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "user_role", nullable = false, length = 100)
    private String userRole;

    @Column(name = "ai_role", nullable = false, length = 100)
    private String aiRole;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tasks", columnDefinition = "jsonb", nullable = false)
    private JsonNode tasks; // List tasks: [{"id": 0, "text": "..."}]

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sample_dialogue", columnDefinition = "jsonb", nullable = false)
    private JsonNode sampleDialogue; // List sample lines: [{"speaker": "AI", "text": "..."}]

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vocabulary", columnDefinition = "jsonb", nullable = false)
    private JsonNode vocabulary; // List vocab: [{"word": "...", "reading": "...", "meaning": "..."}]

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grammar", columnDefinition = "jsonb", nullable = false)
    private JsonNode grammar; // List grammar points: [{"pattern": "...", "meaning": "..."}]

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tips", columnDefinition = "jsonb", nullable = false)
    private JsonNode tips; // List tips: ["..."]

    @Column(name = "is_premium", nullable = false)
    @Builder.Default
    private boolean isPremium = false;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
}
