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
 * Ví dụ câu riêng cho 1 từ vựng trong bài học, do giáo viên tự thêm/chọn.
 * Có thể khác hoàn toàn với ví dụ trong từ điển ({@code vocabulary_examples}).
 */
@Entity
@Table(name = "lesson_vocabulary_examples")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonVocabularyExample extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocab_item_id", nullable = false)
    private LessonVocabularyItem vocabItem;

    @Column(name = "example_jp", nullable = false, columnDefinition = "text")
    private String exampleJp;

    @Column(name = "example_vi", nullable = false, columnDefinition = "text")
    private String exampleVi;

    @Column(name = "example_romaji", columnDefinition = "text")
    private String exampleRomaji;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;
}
