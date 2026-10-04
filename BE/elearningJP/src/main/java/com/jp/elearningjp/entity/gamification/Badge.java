package com.jp.elearningjp.entity.gamification;

import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;


@Entity
@Table(name = "badges")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge extends SoftDeletableEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "icon_url", nullable = false, length = 500)
    private String iconUrl;

    @Builder.Default
    @Column(name = "xp_reward", nullable = false)
    private Integer xpReward = 50;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

}
