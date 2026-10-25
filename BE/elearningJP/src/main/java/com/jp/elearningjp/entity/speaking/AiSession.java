package com.jp.elearningjp.entity.speaking;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.AiPracticeMode;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity lưu phiên luyện nói của người dùng (tình huống hoặc tự do).
 */
@Entity
@Table(name = "ai_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSession extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private AiScenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private AiPracticeMode mode; // SCENARIO hoặc FREE

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "completed_tasks", columnDefinition = "jsonb", nullable = false)
    private JsonNode completedTasks; // List ID nhiệm vụ đã hoàn thành: [0, 1]

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<AiMessage> messages = new ArrayList<>();
}
