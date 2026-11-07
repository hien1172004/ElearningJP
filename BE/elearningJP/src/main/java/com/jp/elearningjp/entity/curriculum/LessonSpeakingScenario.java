package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.entity.speaking.AiScenario;
import com.jp.elearningjp.shared.persitence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

/**
 * Entity liên kết giữa Bài học ({@link Lesson}) và Kịch bản hội thoại AI ({@link AiScenario}).
 * Một bài học có thể chứa nhiều đoạn kịch bản hội thoại (1-N) theo thứ tự {@code orderIndex}.
 */
@Entity
@Table(name = "lesson_speaking_scenarios")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonSpeakingScenario extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private AiScenario scenario;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 1;

    @Column(name = "min_turns", nullable = false)
    @Builder.Default
    private Integer minTurns = 3;
}
