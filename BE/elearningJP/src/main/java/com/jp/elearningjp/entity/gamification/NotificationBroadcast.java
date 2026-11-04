package com.jp.elearningjp.entity.gamification;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.BroadcastAudience;
import com.jp.elearningjp.shared.persitence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** Một lần gửi thông báo hàng loạt (người nhận lưu ở bảng notifications). */
@Entity
@Table(name = "notification_broadcasts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationBroadcast extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience", nullable = false, length = 30)
    private BroadcastAudience audience;

    @Column(name = "role", length = 20)
    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "link", length = 300)
    private String link;

    @Builder.Default
    @Column(name = "recipient_count", nullable = false)
    private Integer recipientCount = 0;
}
