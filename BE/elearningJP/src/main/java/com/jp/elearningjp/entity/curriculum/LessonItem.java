package com.jp.elearningjp.entity.curriculum;


import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.content.ReadingPassage;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "lesson_items")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonItem extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id")
    private Vocabulary vocab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private WritingCharacter character;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grammar_id")
    private GrammarPoint grammar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_id")
    private ReadingPassage reading;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

}
