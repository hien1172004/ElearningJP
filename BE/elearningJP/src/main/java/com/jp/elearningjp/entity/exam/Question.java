package com.jp.elearningjp.entity.exam;


import com.jp.elearningjp.entity.content.ReadingPassage;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.JlptLevel;
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

    @Column(name = "audio_start_time")
    private Integer audioStartTime;

    @Column(name = "audio_end_time")
    private Integer audioEndTime;

    @Column(name = "transcript_jp", columnDefinition = "text")
    private String transcriptJp;

    @Column(name = "transcript_vi", columnDefinition = "text")
    private String transcriptVi;

    @Column(name = "star_position")
    private Short starPosition;

    @Column(name = "has_visual_options", nullable = false)
    private boolean hasVisualOptions = false;

    @Column(name = "explanation_vi", columnDefinition = "text")
    private String explanationVi;

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
