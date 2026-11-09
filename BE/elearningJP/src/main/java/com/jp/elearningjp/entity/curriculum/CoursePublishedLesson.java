package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.shared.persitence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Nội dung 1 bài học trong 1 phiên bản xuất bản — bất biến sau khi tạo. */
@Entity
@Table(name = "course_published_lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoursePublishedLesson extends BaseEntity {

    @Column(name = "published_version_id", nullable = false)
    private Long publishedVersionId;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    /** JSON {@code LessonSnapshot}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;
}
