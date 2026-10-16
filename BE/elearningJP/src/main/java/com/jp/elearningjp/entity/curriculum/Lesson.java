package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

/**
 * Entity {@code Lesson} — bài học thuộc một {@link Course}.
 *
 * <p>SAU KHI REFACTOR (V14): bảng {@code lessons} chỉ còn giữ thông tin khung
 * của bài học (title, thứ tự, archived). Toàn bộ nội dung (Kanji / Vocabulary /
 * Grammar / Quiz) được lưu trong {@link LessonSection} và các bảng con
 * (lesson_kanji_items, lesson_vocabulary_items, lesson_grammar_items,
 * lesson_quizzes).</p>
 *
 * <p>Loại bỏ hoàn toàn các trường cũ: {@code lessonType}, {@code contentMarkdown},
 * {@code durationMinutes}.</p>
 */
@Entity
@Table(name = "lessons")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Builder.Default
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<LessonSection> sections = new ArrayList<>();

    public void addSection(LessonSection child) {
        sections.add(child);
        child.setLesson(this);
    }

    public void removeSection(LessonSection child) {
        sections.remove(child);
        child.setLesson(null);
    }
}
