package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.LessonSectionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Thay đổi của bản đang soạn so với bản học viên đang học (phiên bản đã duyệt gần nhất) —
 * để ADMIN duyệt đúng phần thay đổi, giáo viên kiểm tra trước khi gửi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseChangesResponse {

    /** null = khóa chưa có bản được duyệt (mọi bài đều là mới). */
    Integer publishedVersionNo;
    Instant publishedAt;
    boolean hasChanges;

    @Builder.Default
    List<LessonChange> lessons = new ArrayList<>();

    @Builder.Default
    List<ExamChange> exams = new ArrayList<>();

    /** ADDED: chưa có ở bản đang chạy · REMOVED: sẽ bị gỡ · MODIFIED: có sửa · UNCHANGED. */
    public enum Status { ADDED, REMOVED, MODIFIED, UNCHANGED }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LessonChange {
        Long lessonId;
        String title;
        /** Thứ tự ở bản đang soạn (null nếu REMOVED) và ở bản đang chạy (null nếu ADDED). */
        Integer orderIndex;
        Integer publishedOrderIndex;
        Status status;
        /** Đổi tiêu đề / thông tin bài. */
        boolean infoChanged;
        /** Đổi vị trí trong khóa. */
        boolean moved;
        @Builder.Default
        List<SectionChange> sections = new ArrayList<>();
        ItemsChange quiz;
        boolean speakingChanged;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SectionChange {
        Long sectionId;
        String title;
        LessonSectionType sectionType;
        Status status;
        ItemsChange items;
    }

    /** Số mục thêm / bỏ / sửa và có đổi thứ tự hay không. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ItemsChange {
        int added;
        int removed;
        int modified;
        boolean reordered;
        /** Đổi thông tin chung (vd tiêu đề quiz, điểm đạt, tên mục). */
        boolean metaChanged;

        public boolean hasChanges() {
            return added > 0 || removed > 0 || modified > 0 || reordered || metaChanged;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ExamChange {
        Long examId;
        String title;
        Status status;
    }
}
