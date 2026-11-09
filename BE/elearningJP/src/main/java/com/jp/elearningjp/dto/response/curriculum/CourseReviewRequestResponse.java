package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/** Một lượt gửi duyệt khóa học. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseReviewRequestResponse {

    Long id;
    Long courseId;
    String courseTitle;
    ReviewRequestStatus status;
    /** Ghi chú của người gửi. */
    String note;
    Long submittedById;
    String submittedByName;
    Instant createdAt;
    /** ADMIN duyệt / từ chối, hoặc người rút yêu cầu. */
    Long reviewedById;
    String reviewedByName;
    /** Lý do từ chối / ghi chú khi duyệt. */
    String reviewNote;
    Instant reviewedAt;
}
