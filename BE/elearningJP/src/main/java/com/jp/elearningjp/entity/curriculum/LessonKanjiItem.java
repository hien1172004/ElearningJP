package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
 * Một chữ Hán được giáo viên chọn cho bài học.
 * Liên kết tới {@link WritingCharacter} (bảng writing_characters trong từ điển).
 */
@Entity
@Table(name = "lesson_kanji_items")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonKanjiItem extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private LessonSection section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false)
    private WritingCharacter character;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Builder.Default
    @OneToMany(mappedBy = "kanjiItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LessonKanjiRelatedVocab> relatedVocabulary = new ArrayList<>();

    public void addRelatedVocab(LessonKanjiRelatedVocab rv) {
        relatedVocabulary.add(rv);
        rv.setKanjiItem(this);
    }

    public void removeRelatedVocab(LessonKanjiRelatedVocab rv) {
        relatedVocabulary.remove(rv);
        rv.setKanjiItem(null);
    }
}
