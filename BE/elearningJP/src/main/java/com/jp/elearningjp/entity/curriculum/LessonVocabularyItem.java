package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.entity.content.Vocabulary;
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
 * Một từ vựng được giáo viên chọn cho bài học.
 * Liên kết tới {@link Vocabulary} (bảng vocabulary trong từ điển).
 */
@Entity
@Table(name = "lesson_vocabulary_items")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonVocabularyItem extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private LessonSection section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocab_id", nullable = false)
    private Vocabulary vocab;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;

    /** Ghi đè hiragana (nếu giáo viên muốn dùng cách đọc khác) */
    @Column(name = "custom_hiragana", length = 100)
    private String customHiragana;

    /** Ghi đè romaji (nếu giáo viên muốn dùng cách viết romaji khác) */
    @Column(name = "custom_romaji", length = 150)
    private String customRomaji;

    /** Ghi đè audio (nếu giáo viên muốn dùng audio riêng) */
    @Column(name = "custom_audio_url", length = 500)
    private String customAudioUrl;

    /** Ghi đè nghĩa (nếu giáo viên muốn giải thích khác) */
    @Column(name = "custom_meaning", columnDefinition = "text")
    private String customMeaning;

    /** Ảnh minh họa riêng cho từ vựng trong bài học này */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Builder.Default
    @OneToMany(mappedBy = "vocabItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<LessonVocabularyExample> examples = new ArrayList<>();

    public void addExample(LessonVocabularyExample ex) {
        examples.add(ex);
        ex.setVocabItem(this);
    }

    public void removeExample(LessonVocabularyExample ex) {
        examples.remove(ex);
        ex.setVocabItem(null);
    }
}
