package com.jp.elearningjp.service.curriculum.snapshot;

import com.jp.elearningjp.dto.response.curriculum.LessonOutlineResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Nội dung JSON của {@code course_published_versions.content}: mục lục bài học học viên đang học.
 * Không chứa dữ liệu theo người dùng (tiến độ được ghép lúc đọc).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSnapshot {

    @Builder.Default
    private int schemaVersion = 1;

    /** Theo thứ tự bài; status / bestScore luôn null. */
    @Builder.Default
    private List<LessonOutlineResponse> lessons = new ArrayList<>();
}
