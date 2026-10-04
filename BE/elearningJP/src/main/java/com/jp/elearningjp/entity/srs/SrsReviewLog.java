package com.jp.elearningjp.entity.srs;

import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.SrsStatus;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "srs_review_logs")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SrsReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "srs_item_id", nullable = false)
    private SrsItem srsItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rating", nullable = false, length = 10)
    private String rating;

    @Column(name = "score_q", nullable = false)
    private Short scoreQ;

    @Column(name = "previous_interval", nullable = false)
    private Integer previousInterval;

    @Column(name = "new_interval", nullable = false)
    private Integer newInterval;

    @Column(name = "previous_ef", nullable = false, precision = 3, scale = 2)
    private BigDecimal previousEf;

    @Column(name = "new_ef", nullable = false, precision = 3, scale = 2)
    private BigDecimal newEf;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private SrsStatus newStatus;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "client_event_id")
    private UUID clientEventId;

    @Builder.Default
    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt = Instant.now();

}
