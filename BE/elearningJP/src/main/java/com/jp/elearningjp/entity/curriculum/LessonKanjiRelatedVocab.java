package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.entity.content.Vocabulary;
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
 * Từ vựng liên quan tới 1 chữ Hán trong bài học, do giáo viên tự chọn.
 * Khác với {@code vocabulary_characters} (đó là quan hệ tự động trong từ điển).
 */
@Entity
@Table(name = "lesson_kanji_related_vocabulary")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonKanjiRelatedVocab extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kanji_item_id", nullable = false)
    private LessonKanjiItem kanjiItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocab_id", nullable = false)
    private Vocabulary vocab;

    @Column(name = "note", columnDefinition = "text")
    private String note;
}
