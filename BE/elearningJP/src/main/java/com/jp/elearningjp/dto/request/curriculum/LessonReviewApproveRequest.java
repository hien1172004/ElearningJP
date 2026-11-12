package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** ADMIN duyệt bài: ghi chú (không bắt buộc) + đánh dấu "thay đổi lớn" để báo học viên đã hoàn thành bài. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonReviewApproveRequest {

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    String note;

    /** true → học viên đã hoàn thành bài nhận thông báo "bài có nội dung mới" (khi khóa đang xuất bản). */
    boolean majorChange;
}
