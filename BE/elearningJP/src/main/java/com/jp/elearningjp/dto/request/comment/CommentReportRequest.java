package com.jp.elearningjp.dto.request.comment;

import com.jp.elearningjp.shared.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Người dùng báo cáo vi phạm một bình luận / trả lời. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentReportRequest {

    @NotNull(message = "Vui lòng chọn lý do báo cáo")
    ReportReason reason;

    @Size(max = 500, message = "Mô tả thêm tối đa 500 ký tự")
    String detail;
}
