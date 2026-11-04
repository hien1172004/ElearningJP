package com.jp.elearningjp.dto.request.notification;

import com.jp.elearningjp.shared.enums.BroadcastAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Gửi thông báo hàng loạt. {@code role} bắt buộc khi audience = ROLE; {@code courseId} khi COURSE_STUDENTS. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BroadcastRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 120, message = "Tiêu đề tối đa 120 ký tự")
    String title;

    @NotBlank(message = "Nội dung không được để trống")
    @Size(max = 1000, message = "Nội dung tối đa 1000 ký tự")
    String body;

    @NotNull(message = "Vui lòng chọn đối tượng nhận")
    BroadcastAudience audience;

    String role;

    Long courseId;

    /** Đường dẫn trong ứng dụng (bắt đầu bằng "/"), mở khi người nhận bấm thông báo. */
    @Size(max = 300, message = "Đường dẫn tối đa 300 ký tự")
    @Pattern(regexp = "^$|^/(?!/)\\S*$", message = "Đường dẫn phải là đường dẫn trong ứng dụng, bắt đầu bằng /")
    String link;
}
