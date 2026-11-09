package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Giáo viên đánh dấu nhận xét đã sửa / chưa sửa. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseReviewCommentResolveRequest {

    @NotNull(message = "Trạng thái đã sửa là bắt buộc")
    Boolean resolved;
}
