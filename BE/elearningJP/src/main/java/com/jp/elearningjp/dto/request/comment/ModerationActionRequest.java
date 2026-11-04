package com.jp.elearningjp.dto.request.comment;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Ghi chú (không bắt buộc) khi ẩn / xóa — gửi kèm trong thông báo cho người viết. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModerationActionRequest {

    @Size(max = 300, message = "Ghi chú tối đa 300 ký tự")
    String note;
}
