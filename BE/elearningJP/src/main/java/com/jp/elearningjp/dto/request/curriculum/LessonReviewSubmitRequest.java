package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Giáo viên gửi một hoặc nhiều bài lên ADMIN duyệt. Mỗi bài kèm {@code draftRevision} đang thấy:
 * bài đã bị sửa sau đó → bài đó nhận xung đột (409 CONCURRENT_MODIFICATION), các bài khác vẫn được gửi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonReviewSubmitRequest {

    @NotEmpty(message = "Chưa chọn bài học nào để gửi duyệt")
    @Size(max = 100, message = "Mỗi lần gửi tối đa 100 bài")
    List<@Valid Item> lessons;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    String note;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Item {

        @NotNull(message = "Thiếu mã bài học")
        Long lessonId;

        @NotNull(message = "Thiếu phiên bản bản nháp của bài")
        @PositiveOrZero(message = "Phiên bản bản nháp không hợp lệ")
        Long draftRevision;
    }
}
