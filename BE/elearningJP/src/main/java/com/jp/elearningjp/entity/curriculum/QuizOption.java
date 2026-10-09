package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "quiz_options")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizOption extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "option_key", nullable = false)
    private Short optionKey; // 1, 2, 3, 4 (A, B, C, D)

    @Column(name = "option_text", nullable = false, columnDefinition = "text")
    private String optionText;

    @Builder.Default
    @Column(name = "is_correct", nullable = false)
    private boolean correct = false;

    @Column(name = "match_key", length = 100)
    private String matchKey; // Dành cho dạng MATCHING (khóa liên kết giữa câu/ô trống và đáp án)
}
