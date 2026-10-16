package com.jp.elearningjp.dto.request.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request tạo {@code CommentReply}.
 *
 * <p>Nếu muốn reply lồng nhau (trả lời 1 reply khác), truyền {@code parentReplyId}.
 * Ngược lại bỏ trống để reply thẳng vào comment gốc.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentReplyCreateRequest {

    @NotBlank(message = "Nội dung reply không được để trống")
    @Size(min = 1, max = 5000, message = "Nội dung reply từ 1 đến 5000 ký tự")
    String content;

    @Positive(message = "parentReplyId phải > 0")
    Long parentReplyId;
}
