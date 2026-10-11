package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;

import java.util.ArrayList;
import java.util.List;

/**
 * Nhóm nội dung trong một bài học.
 * Mỗi lesson được chia thành nhiều section theo {@link LessonSectionType}
 * (KANJI, VOCABULARY, GRAMMAR, TEST) để dễ quản lý thứ tự và phân loại nội dung.
 */
@Entity
@Table(name = "lesson_sections")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonSection extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 20)
    private LessonSectionType sectionType;

    @Column(name = "title", length = 255)
    private String title;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;

    // ===== KANJI items =====
    @Builder.Default
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<LessonKanjiItem> kanjiItems = new ArrayList<>();

    // ===== VOCABULARY items =====
    @Builder.Default
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<LessonVocabularyItem> vocabularyItems = new ArrayList<>();

    // ===== GRAMMAR items =====
    @Builder.Default
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<LessonGrammarItem> grammarItems = new ArrayList<>();

    // ===== TEST (Quiz) — mỗi TEST section có tối đa 1 quiz =====
    @Builder.Default
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Quiz> quizzes = new ArrayList<>();

    // ===== Helper methods =====
    public void addKanjiItem(LessonKanjiItem item) {
        kanjiItems.add(item);
        item.setSection(this);
    }

    public void removeKanjiItem(LessonKanjiItem item) {
        kanjiItems.remove(item);
        item.setSection(null);
    }

    public void addVocabularyItem(LessonVocabularyItem item) {
        vocabularyItems.add(item);
        item.setSection(this);
    }

    public void removeVocabularyItem(LessonVocabularyItem item) {
        vocabularyItems.remove(item);
        item.setSection(null);
    }

    public void addGrammarItem(LessonGrammarItem item) {
        grammarItems.add(item);
        item.setSection(this);
    }

    public void removeGrammarItem(LessonGrammarItem item) {
        grammarItems.remove(item);
        item.setSection(null);
    }

    public void addQuiz(Quiz quiz) {
        quizzes.add(quiz);
        quiz.setSection(this);
    }

    public void removeQuiz(Quiz quiz) {
        quizzes.remove(quiz);
        quiz.setSection(null);
    }
}
