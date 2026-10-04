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

}
