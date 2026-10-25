package com.jp.elearningjp.entity.speaking;

import com.jp.elearningjp.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity theo dõi số token/lượt sử dụng AI Speaking theo ngày của từng người dùng.
 */
@Entity
@Table(name = "ai_usage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsage {

    @EmbeddedId
    private AiUsageId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tokens_used", nullable = false)
    @Builder.Default
    private Integer tokensUsed = 0;
}
