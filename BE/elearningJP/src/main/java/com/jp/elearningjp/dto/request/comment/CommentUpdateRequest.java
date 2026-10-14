package com.jp.elearningjp.dto.request.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request cập nhật nội dung {@code Comment} (chỉ chủ sở hữu mới được sửa).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentUpdateRequest {

    @NotBlank(message = "Nội dung bình luận không được để trống")
    @Size(min = 1, max = 5000, message = "Nội dung bình luận từ 1 đến 5000 ký tự")
    String content;
}
