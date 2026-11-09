package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.persitence.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Giáo viên được ADMIN giao quản lý nội dung một khóa học.
 * Gỡ giáo viên khỏi khóa = xóa dòng (bài học họ đã tạo vẫn giữ nguyên trong khóa).
 */
@Entity
@Table(name = "course_teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseTeacher extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    /** ADMIN đã giao; null nếu tài khoản đó đã bị xóa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;
}
