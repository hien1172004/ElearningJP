package com.jp.elearningjp.entity.gamification;

import com.jp.elearningjp.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "user_daily_stats")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDailyStats {

    @Builder.Default
    @EmbeddedId
    private UserDailyStatsId id = new UserDailyStatsId();

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @Column(name = "study_seconds", nullable = false)
    private Integer studySeconds = 0;

    @Builder.Default
    @Column(name = "cards_reviewed", nullable = false)
    private Integer cardsReviewed = 0;

    @Builder.Default
    @Column(name = "new_items_learned", nullable = false)
    private Integer newItemsLearned = 0;

    @Builder.Default
    @Column(name = "xp_earned", nullable = false)
    private Integer xpEarned = 0;

    // -------------------------------------------------------------------------
    // Cot bo sung o V25 — phuc vu trang Progress (Khoi 1/2/6)
    // -------------------------------------------------------------------------

    /**
     * So lan tra tu dien trong ngay. Dinh nghia "1 tu da hoc" cho trang Progress
     * la 1 lan tra tu dien, nen {@code lookups} chinh la so tu da hoc.
     *
     * <p>Khong co tran - ghi nhan moi lan tra deu tang.</p>
     */
    @Builder.Default
    @Column(name = "lookups", nullable = false)
    private Integer lookups = 0;

    /**
     * So lan tra tu da duoc tinh diem vao leaderboard.
     * Co tran theo ngay lay tu bang {@code app_config} (key: {@code lookup_ceiling}).
     * Chi tang khi {@code lookups_scored < lookup_ceiling}.</p>
     */
    @Builder.Default
    @Column(name = "lookups_scored", nullable = false)
    private Integer lookupsScored = 0;

    /**
     * So bai hoan thanh trong ngay (chi dem lan dau tien moi bai).
     * Dung cho leaderboard: lesson hoan thanh = +1 diem.
     */
    @Builder.Default
    @Column(name = "lessons_completed", nullable = false)
    private Integer lessonsCompleted = 0;

    /** Moc tieu mac dinh 5 tu/ngay (user co the doi o profile - phase sau). */
    @Builder.Default
    @Column(name = "goal_words", nullable = false)
    private Integer goalWords = 5;

    /** Da dat muc tieu chua ({@code lookups >= goalWords}). */
    @Builder.Default
    @Column(name = "goal_met", nullable = false)
    private Boolean goalMet = false;

    /** Thoi diem tra tu gan nhat — dung cho tinh streak va debug. */
    @Column(name = "last_lookup_at")
    private Instant lastLookupAt;
}
