package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.CourseReviewStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Trạng thái duyệt của 1 bài (chỉ cho ADMIN / giáo viên được giao). Không nằm trong
 * {@code LessonOutlineResponse} để snapshot bản live không đổi dạng JSON.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonReviewStateResponse {

    Long lessonId;
    String title;
    Integer orderIndex;
    /** EDITING | PENDING_REVIEW (giáo viên đã gửi duyệt bài này — nháp giáo viên chỉ đọc; ADMIN phải duyệt / từ chối trước khi lưu bài). */
    CourseReviewStatus reviewStatus;
    /** Nháp phía người xem khác bản live (có gì để gửi duyệt / lưu). */
    boolean hasUnpublishedChanges;
    /** Gửi kèm khi gửi duyệt bài này. */
    long draftRevision;
    /** Bài đã có trong bản live (false = bài mới, chưa từng duyệt). */
    boolean inPublishedVersion;
    String draftUpdatedByName;
    Instant draftUpdatedAt;
    /** Yêu cầu đang chờ (nếu có). */
    Long pendingRequestId;
    String pendingSubmittedByName;
    Instant pendingSubmittedAt;
    /**
     * Nháp phía người xem đã cũ (V65): bản học viên đang học đã đổi sau khi nháp được tạo → phải "cập nhật bản mới"
     * trước khi gửi duyệt / lưu.
     */
    boolean outdated;
    /** Phía người xem còn nháp cũ chỉ đọc (giữ lại khi cập nhật bản mới) để chép lại phần còn thiếu. */
    boolean hasArchivedDraft;
}
