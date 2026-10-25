package com.jp.elearningjp.entity.speaking;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.AiMessageRole;
import com.jp.elearningjp.shared.persitence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity lưu từng lượt thoại trong phiên luyện nói AI.
 */
@Entity
@Table(name = "ai_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AiSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private AiMessageRole role; // USER hoặc AI

    @Column(name = "text_ja", nullable = false, columnDefinition = "text")
    private String textJa;

    @Column(name = "text_vi", columnDefinition = "text")
    private String textVi;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correction", columnDefinition = "jsonb")
    private JsonNode correction; // {"original": "...", "fixed": "...", "note_vi": "..."}
}
