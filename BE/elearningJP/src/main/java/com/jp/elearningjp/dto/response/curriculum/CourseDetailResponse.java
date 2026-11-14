package com.jp.elearningjp.dto.response.curriculum;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jp.elearningjp.shared.enums.CourseReviewStatus;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseDetailResponse {

    Long id;
    /** Version để gửi lại khi cập nhật (optimistic locking). */
    Long version;
    String title;
    String slug;
    JlptLevel jlptLevel;
    String description;
    String thumbnailUrl;
    Boolean published;
    Boolean isPremium;
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
    /** Phiên bản học viên đang học (chỉ trả cho ADMIN / giáo viên được giao; null = chưa có). */
    Integer publishedVersionNo;
    Instant publishedVersionAt;
    Instant createdAt;

    // Thông tin người tạo
    Long createdByUserId;
    String createdByName;

    // Phân quyền & tiến độ
    // Lombok sinh getter isEnrolled() → Jackson mặc định đặt tên "enrolled"; FE đọc "isEnrolled"
    @JsonProperty("isEnrolled")
    boolean isEnrolled;              // User hiện tại đã đăng ký chưa
    boolean canManage;               // User hiện tại là ADMIN hoặc giáo viên được giao (xem trước không cần đăng ký)
    BigDecimal progressPercent;      // % hoàn thành khóa học (nếu đã đăng ký)
    Integer completedLessonsCount;   // Số bài học đã hoàn thành
    Integer totalLessons;            // Tổng số bài học
    Long totalEnrollments;           // Tổng số học viên đã đăng ký

    // Danh sách mục lục các bài học (Outline)
    List<LessonOutlineResponse> lessons;
}