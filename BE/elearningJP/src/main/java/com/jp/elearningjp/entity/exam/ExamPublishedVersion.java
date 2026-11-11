package com.jp.elearningjp.entity.exam;

import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.persitence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Một phiên bản xuất bản (snapshot) của đề thi gắn khóa — bất biến sau khi tạo. */
@Entity
@Table(name = "exam_published_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamPublishedVersion extends BaseEntity {

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    /** JSON {@code ExamSnapshot}: câu hỏi, đáp án, điểm, nhóm điểm. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private User publishedBy;

    /** Ghi chú tóm tắt nội dung thay đổi giữa các phiên bản (V1 -> V2). */
    @Column(name = "changelog", length = 1000)
    private String changelog;
}
