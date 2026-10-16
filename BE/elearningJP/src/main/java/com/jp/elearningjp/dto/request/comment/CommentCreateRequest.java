package com.jp.elearningjp.dto.request.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request tạo mới {@code Comment}.
 *
 * <p>{@code targetType} + {@code targetId} nằm trong URL path, không nằm trong body.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentCreateRequest {

    @NotBlank(message = "Nội dung bình luận không được để trống")
    @Size(min = 1, max = 5000, message = "Nội dung bình luận từ 1 đến 5000 ký tự")
    String content;
}
