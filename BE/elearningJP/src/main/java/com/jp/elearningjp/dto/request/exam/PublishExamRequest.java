package com.jp.elearningjp.dto.request.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishExamRequest {
    /** Ghi chú tóm tắt nội dung thay đổi giữa các phiên bản (V1 -> V2). */
    private String changelog;

    /** Tùy chọn gửi thông báo tới các học viên đã từng thi đề này. */
    @Builder.Default
    private Boolean notifyLearners = false;
}

