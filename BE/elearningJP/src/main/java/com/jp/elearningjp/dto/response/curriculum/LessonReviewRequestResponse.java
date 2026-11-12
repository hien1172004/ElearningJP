package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/** Một lượt gửi 1 bài học lên duyệt. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonReviewRequestResponse {

    Long id;
    Long lessonId;
    String lessonTitle;
    Long courseId;
    String courseTitle;
    /** Các bài gửi cùng lượt có chung mã này. */
    UUID batchId;
    ReviewRequestStatus status;
    /** Ghi chú của người gửi. */
    String note;
    Long submittedById;
    String submittedByName;
    Instant createdAt;
    /** ADMIN duyệt / từ chối, hoặc giáo viên rút yêu cầu. */
    Long reviewedById;
    String reviewedByName;
    /** Lý do từ chối / ghi chú khi duyệt. */
    String reviewNote;
    Instant reviewedAt;
    /** Duyệt kèm "thay đổi lớn" (đã báo học viên đã học bài). */
    boolean majorChange;
}
