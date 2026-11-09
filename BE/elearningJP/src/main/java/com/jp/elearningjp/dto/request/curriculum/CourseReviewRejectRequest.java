package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** ADMIN từ chối yêu cầu duyệt — bắt buộc ghi lý do để giáo viên sửa tiếp. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseReviewRejectRequest {

    @NotBlank(message = "Vui lòng nhập lý do từ chối")
    @Size(max = 1000, message = "Lý do từ chối tối đa 1000 ký tự")
    String reason;
}
