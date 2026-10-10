package com.jp.elearningjp.entity.content;

import com.jp.elearningjp.shared.enums.CharacterType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.ReviewStatus;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "writing_characters")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WritingCharacter extends SoftDeletableEntity {

    @Column(name = "character", nullable = false, length = 4)
    private String character;

    @Enumerated(EnumType.STRING)
    @Column(name = "char_type", nullable = false, length = 20)
    private CharacterType charType;

    @Column(name = "stroke_count", nullable = false)
    private Integer strokeCount;

    @Column(name = "onyomi", columnDefinition = "text[]")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String[] onyomi;

    @Column(name = "kunyomi", columnDefinition = "text[]")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String[] kunyomi;

    @Column(name = "han_viet", length = 100)
    private String hanViet;

    @Column(name = "meaning_vi", length = 255)
    private String meaningVi;

    @Column(name = "romaji", length = 20)
    private String romaji;

    @Enumerated(EnumType.STRING)
    @Column(name = "jlpt_level", length = 2)
    private JlptLevel jlptLevel;

    @Column(name = "radicals", columnDefinition = "text[]")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String[] radicals;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "meaning_en", length = 255)
    private String meaningEn;

    @Column(name = "data_source", length = 100)
    private String dataSource;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus = ReviewStatus.DRAFT;

    @Builder.Default
    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("strokeNumber ASC")
    private List<CharacterStroke> strokes = new ArrayList<>();

    public void addStroke(CharacterStroke child) {
        strokes.add(child);
        child.setCharacter(this);
    }

    public void removeStroke(CharacterStroke child) {
        strokes.remove(child);
        child.setCharacter(null);
    }

    public List<String> getOnyomi() {
        return onyomi != null ? java.util.Arrays.asList(onyomi) : java.util.Collections.emptyList();
    }

    public void setOnyomi(List<String> list) {
        this.onyomi = (list != null) ? list.toArray(new String[0]) : null;
    }

    public List<String> getKunyomi() {
        return kunyomi != null ? java.util.Arrays.asList(kunyomi) : java.util.Collections.emptyList();
    }

    public void setKunyomi(List<String> list) {
        this.kunyomi = (list != null) ? list.toArray(new String[0]) : null;
    }

    public List<String> getRadicals() {
        return radicals != null ? java.util.Arrays.asList(radicals) : java.util.Collections.emptyList();
    }

    public void setRadicals(List<String> list) {
        this.radicals = (list != null) ? list.toArray(new String[0]) : null;
    }
}
