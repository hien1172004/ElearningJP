package com.jp.elearningjp.entity.content;

import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.ReviewStatus;
import com.jp.elearningjp.shared.enums.Visibility;
import com.jp.elearningjp.shared.enums.VocabularySource;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "vocabulary")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vocabulary extends SoftDeletableEntity {

    @Column(name = "word", nullable = false, length = 100)
    private String word;

    @Column(name = "hiragana", nullable = false, length = 100)
    private String hiragana;

    @Builder.Default
    @Column(name = "sense_no", nullable = false)
    private Short senseNo = (short) 1;

    @Column(name = "meaning_vi", columnDefinition = "text")
    private String meaningVi;

    @Enumerated(EnumType.STRING)
    @Column(name = "jlpt_level", length = 2)
    private JlptLevel jlptLevel;

    @Column(name = "part_of_speech", length = 50)
    private String partOfSpeech;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Builder.Default
    @Column(name = "is_tts", nullable = false)
    private boolean tts = false;

    @Column(name = "example_jp", columnDefinition = "text")
    private String exampleJp;

    @Column(name = "example_vi", columnDefinition = "text")
    private String exampleVi;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private VocabularySource sourceType = VocabularySource.CORE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 10)
    private Visibility visibility = Visibility.PUBLIC;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus = ReviewStatus.PUBLISHED;

    @Builder.Default
    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "meaning_en", columnDefinition = "text")
    private String meaningEn;

    @Column(name = "data_source", length = 100)
    private String dataSource;


}
