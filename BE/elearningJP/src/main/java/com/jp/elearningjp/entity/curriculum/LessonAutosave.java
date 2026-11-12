package com.jp.elearningjp.entity.curriculum;

import com.jp.elearningjp.shared.persitence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Bản chưa lưu của 1 form trong 1 bài, theo từng người (V63). Chỉ đọc qua entity; ghi bằng upsert
 * ({@code LessonAutosaveRepository.upsert}) để không đụng tới nội dung / version của bài.
 */
@Entity
@Table(name = "lesson_autosaves")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonAutosave extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    /** Do FE đặt, vd "vocab:345", "vocab:new:12", "quiz:new". */
    @Column(name = "form_key", nullable = false, length = 100)
    private String formKey;

    /** Giá trị form (JSON) — FE tự hiểu theo form_key. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;
}
