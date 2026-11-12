package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.CourseReviewStatus;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseResponse {

    Long id;
    /** Version để gửi lại khi cập nhật (optimistic locking). */
    Long version;
    String title;
    String slug;
    JlptLevel jlptLevel;
    String description;
    String thumbnailUrl;
    boolean published;
    /** Trạng thái bản đang soạn: EDITING | PENDING_REVIEW. */
    CourseReviewStatus reviewStatus;
    /** Bản đang soạn có thay đổi chưa được duyệt / xuất bản. */
    Boolean hasUnpublishedChanges;
    /** Người / thời điểm sửa bản đang soạn gần nhất (null = chưa rõ). */
    Long draftUpdatedById;
    String draftUpdatedByName;
    Instant draftUpdatedAt;
    /** Phiên bản đã duyệt gần nhất (null = chưa từng được duyệt → ADMIN chưa mở được khóa). */
    Long publishedVersionId;
    /** Số bài đang chờ duyệt theo bài (chỉ trả cho trang quản lý; null ở trang công khai). */
    Long pendingLessonReviews;
    Integer totalLessons;
    /** Số học viên đang đăng ký (bỏ đăng ký / tài khoản đã xóa) */
    Long totalEnrollments;
    Instant createdAt;
}
