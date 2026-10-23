package com.jp.elearningjp.entity.gamification;

import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.LookupTargetType;
import com.jp.elearningjp.shared.enums.StudyEventType;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;

import java.time.Instant;

/**
 * Su kien hoc tap append-only.
 *
 * <p>Dung cho 2 muc dich:</p>
 * <ol>
 *   <li>Tinh "so tu da hoc" = COUNT(event_type = LOOKUP)</li>
 *   <li>Top-lookups (Khoi 4) = GROUP BY target_id ORDER BY COUNT(*) DESC</li>
 * </ol>
 *
 * <p>Khong dung {@code SoftDeletableEntity} vi bang nay la append-only
 * (khong bao gio xoa mem). Chi co {@code created_at}, khong {@code updated_at}/{@code version}.</p>
 */
@Entity
@Table(name = "study_events")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 24)
    private StudyEventType eventType;

    /** Loai doi tuong tra cuu. NULL voi LESSON_COMPLETE / QUIZ_SUBMIT / HEARTBEAT. */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 16)
    private LookupTargetType targetType;

    /**
     * ID cua doi tuong da tra cuu.
     * - VOCAB   -> {@code vocabulary.id}
     * - GRAMMAR -> {@code grammar_points.id}
     * - KANJI   -> NULL (chu Han la chuoi, khong phai numeric id)
     */
    @Column(name = "target_id")
    private Long targetId;

    @Builder.Default
    @Column(name = "xp_awarded", nullable = false)
    private Integer xpAwarded = 0;

    /** Chuoi tham chieu (vd: kanji character, lesson id dang text). */
    @Column(name = "source_ref", length = 100)
    private String sourceRef;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}