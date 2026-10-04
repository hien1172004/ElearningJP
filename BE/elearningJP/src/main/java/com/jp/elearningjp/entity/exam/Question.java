package com.jp.elearningjp.entity.exam;


import com.jp.elearningjp.entity.content.ReadingPassage;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.ReviewStatus;
import com.jp.elearningjp.shared.enums.SkillType;
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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "questions")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
public class Question extends SoftDeletableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "jlpt_level", nullable = false, length = 2)
    private JlptLevel jlptLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", nullable = false, length = 30)
    private SkillType skillType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mondai_type_id", nullable = false)
    private MondaiType mondaiType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passage_id")
    private ReadingPassage passage;

    @Column(name = "question_text", nullable = false, columnDefinition = "text")
    private String questionText;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "audio_play_limit", nullable = false)
    private Integer audioPlayLimit = 1;

    @Column(name = "transcript_jp", columnDefinition = "text")
    private String transcriptJp;

    @Column(name = "explanation", columnDefinition = "text")
    private String explanation;

    @Column(name = "ai_solved", nullable = false)
    private boolean aiSolved = false;

    @Column(name = "difficulty")
    private Short difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus = ReviewStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb", nullable = false)
    private List<String> tags = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;


    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionKey ASC")
    private List<QuestionOption> options = new ArrayList<>();

    public void addOption(QuestionOption child) {
        options.add(child);
        child.setQuestion(this);
    }

    public void removeOption(QuestionOption child) {
        options.remove(child);
        child.setQuestion(null);
    }

}
