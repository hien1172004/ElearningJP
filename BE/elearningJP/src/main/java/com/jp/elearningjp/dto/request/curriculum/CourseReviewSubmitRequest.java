package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** Giáo viên gửi khóa học lên ADMIN duyệt (ghi chú không bắt buộc). Cũng dùng cho ghi chú khi ADMIN duyệt. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseReviewSubmitRequest {

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    String note;
}
