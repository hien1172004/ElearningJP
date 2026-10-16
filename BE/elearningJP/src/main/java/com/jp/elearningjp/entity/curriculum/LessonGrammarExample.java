package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;

/**
 * Ví dụ câu riêng cho 1 điểm ngữ pháp trong bài học, do giáo viên tự thêm.
 * Có thể khác hoàn toàn với {@code examplesJson} trong {@link com.jp.elearningjp.entity.content.GrammarPoint}.
 */
@Entity
@Table(name = "lesson_grammar_examples")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonGrammarExample extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grammar_item_id", nullable = false)
    private LessonGrammarItem grammarItem;

    @Column(name = "example_jp", nullable = false, columnDefinition = "text")
    private String exampleJp;

    @Column(name = "example_vi", nullable = false, columnDefinition = "text")
    private String exampleVi;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;
}
