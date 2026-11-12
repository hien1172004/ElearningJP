package com.jp.elearningjp.dto.response.curriculum;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

/**
 * Kết quả gửi duyệt nhiều bài: mỗi bài xử lý độc lập (bài này lỗi không làm hỏng bài khác).
 * Bài lỗi kèm mã + message như khi gọi riêng (vd: 9001 xung đột — tải bản mới nhất rồi gửi lại bài đó).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonReviewSubmitResponse {

    UUID batchId;
    int submittedCount;
    int failedCount;
    List<Item> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Item {
        Long lessonId;
        boolean submitted;
        /** Yêu cầu vừa tạo (khi thành công). */
        Long requestId;
        /** Mã lỗi (khi thất bại), cùng bảng mã ErrorCode. */
        Integer code;
        String message;
    }
}
