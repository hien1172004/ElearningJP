package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/** Nhận xét duyệt (gắn với cả khóa / bài / mục). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseReviewCommentResponse {

    Long id;
    Long reviewRequestId;
    /** Trạng thái lượt duyệt chứa nhận xét (PENDING: ADMIN còn thêm / xóa được). */
    ReviewRequestStatus reviewStatus;
    Long lessonId;
    String lessonTitle;
    Long sectionId;
    String sectionTitle;
    String content;
    Long authorId;
    String authorName;
    boolean resolved;
    String resolvedByName;
    Instant resolvedAt;
    Instant createdAt;
}
