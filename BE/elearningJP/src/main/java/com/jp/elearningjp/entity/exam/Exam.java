package com.jp.elearningjp.entity.exam;


import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.ExamType;
import com.jp.elearningjp.shared.enums.JlptLevel;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "exams")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false, length = 30)
    private ExamType examType;

    @Enumerated(EnumType.STRING)
    @Column(name = "jlpt_level", nullable = false, length = 2)
    private JlptLevel jlptLevel;

    @Column(name = "total_duration_minutes", nullable = false)
    private Integer totalDurationMinutes;

    @Builder.Default
    @Column(name = "total_scaled_max_score", nullable = false)
    private Integer totalScaledMaxScore = 180;

    @Column(name = "overall_passing_score")
    private Integer overallPassingScore;

    /** do trigger của DB cập nhật, chỉ đọc */
    @Column(name = "total_questions", nullable = false, insertable = false, updatable = false)
    private Integer totalQuestions;

    @Column(name = "source_file_name", length = 255)
    private String sourceFileName;

    @Builder.Default
    @Column(name = "is_scanned_ocr", nullable = false)
    private boolean scannedOcr = false;

    @Builder.Default
    @Column(name = "is_published", nullable = false)
    private boolean published = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_for_user_id")
    private User createdForUser;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Builder.Default
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamScoreGroup> scoreGroups = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ExamSection> sections = new ArrayList<>();

    public void addScoreGroup(ExamScoreGroup child) {
        scoreGroups.add(child);
        child.setExam(this);
    }

    public void removeScoreGroup(ExamScoreGroup child) {
        scoreGroups.remove(child);
        child.setExam(null);
    }

    public void addSection(ExamSection child) {
        sections.add(child);
        child.setExam(this);
    }

    public void removeSection(ExamSection child) {
        sections.remove(child);
        child.setExam(null);
    }


}
