package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

import java.util.ArrayList;
import java.util.List;

/**
 * Bài kiểm tra trắc nghiệm cuối bài học.
 * <p>
 * <b>Refactor V20:</b> Quiz chỉ liên kết trực tiếp với {@link Lesson} qua
 * {@code lesson_id} (UNIQUE). 1 lesson có tối đa 1 quiz. Không còn qua
 * Section trung gian.
 * </p>
 *
 * <p><b>Quy ước nghiệp vụ:</b></p>
 * <ul>
 *   <li>1 quiz gắn với đúng 1 lesson.</li>
 *   <li>1 lesson chỉ có 1 quiz (UNIQUE constraint).</li>
 *   <li>Sau V20/V21: section TEST đã bị loại bỏ — quiz tạo/quản lý độc lập
 *       với các section KANJI / VOCABULARY / GRAMMAR.</li>
 * </ul>
 */
@Entity
@Table(name = "lesson_quizzes")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz extends SoftDeletableEntity {

    /**
     * Bài học chứa quiz này.
     * UNIQUE constraint đảm bảo 1 lesson = 1 quiz.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Builder.Default
    @Column(name = "passing_score_percent", nullable = false)
    private Integer passingScorePercent = 80;

    @Builder.Default
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    public void addQuestion(QuizQuestion question) {
        questions.add(question);
        question.setQuiz(this);
    }

    public void removeQuestion(QuizQuestion question) {
        questions.remove(question);
        question.setQuiz(null);
    }
}
