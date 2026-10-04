package com.jp.elearningjp.entity.exam;

import com.jp.elearningjp.shared.enums.SessionType;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;

/** Ánh xạ bảng {@code exam_sections}. */
@Entity
@Table(name = "exam_sections")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ExamSection extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "score_group_id")
    private ExamScoreGroup scoreGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 30)
    private SessionType sessionType;

    @Column(name = "section_title", nullable = false, length = 255)
    private String sectionTitle;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Builder.Default
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionNumber ASC")
    private List<ExamSectionQuestion> questions = new ArrayList<>();

    public void addQuestion(ExamSectionQuestion child) {
        questions.add(child);
        child.setSection(this);
        child.setExam(this.exam);
    }

    public void removeQuestion(ExamSectionQuestion child) {
        questions.remove(child);
        child.setSection(null);
    }

}
