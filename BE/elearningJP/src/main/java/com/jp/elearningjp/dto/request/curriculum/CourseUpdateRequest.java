package com.jp.elearningjp.dto.request.curriculum;

import com.jp.elearningjp.shared.enums.JlptLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseUpdateRequest {

    /**
     * Version bản ghi client đã đọc (optimistic locking). Lệch → 409 CONCURRENT_MODIFICATION.
     * Không gửi → bỏ qua kiểm tra.
     */
    Long version;

    @NotBlank(message = "Tiêu đề khóa học không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    String title;

    @NotNull(message = "Cấp độ JLPT không được để trống")
    JlptLevel jlptLevel;

    String description;

    @Size(max = 500, message = "Link ảnh thumbnail tối đa 500 ký tự")
    String thumbnailUrl;

    Boolean isPremium;

    // Không có field published: sửa thông tin giữ nguyên trạng thái xuất bản hiện tại,
    // bật/tắt xuất bản chỉ qua endpoint publish riêng.
}
