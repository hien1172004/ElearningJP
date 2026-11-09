package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** ADMIN thêm nhận xét vào lượt duyệt đang chờ. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseReviewCommentRequest {

    /** Bài được nhận xét; bỏ trống = nhận xét chung cho cả khóa. */
    Long lessonId;

    /** Mục (section) trong bài; bỏ trống = cả bài. Cần có lessonId. */
    Long sectionId;

    @NotBlank(message = "Nội dung nhận xét không được để trống")
    @Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự")
    String content;
}
