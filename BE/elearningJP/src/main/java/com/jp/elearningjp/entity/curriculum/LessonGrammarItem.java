package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.entity.content.GrammarPoint;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;

import java.util.ArrayList;
import java.util.List;

/**
 * Một điểm ngữ pháp được giáo viên chọn cho bài học.
 * Liên kết tới {@link GrammarPoint} (bảng grammar_points trong từ điển).
 */
@Entity
@Table(name = "lesson_grammar_items")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonGrammarItem extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private LessonSection section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grammar_id", nullable = false)
    private GrammarPoint grammar;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;

    /** Ghi đè nghĩa (nếu giáo viên muốn giải thích khác) */
    @Column(name = "custom_meaning", columnDefinition = "text")
    private String customMeaning;

    /** Ghi đè cấu trúc (nếu giáo viên muốn diễn đạt khác) */
    @Column(name = "custom_structure", columnDefinition = "text")
    private String customStructure;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Builder.Default
    @OneToMany(mappedBy = "grammarItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<LessonGrammarExample> examples = new ArrayList<>();

    public void addExample(LessonGrammarExample ex) {
        examples.add(ex);
        ex.setGrammarItem(this);
    }

    public void removeExample(LessonGrammarExample ex) {
        examples.remove(ex);
        ex.setGrammarItem(null);
    }
}
