package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseDetailResponse {

    Long id;
    String title;
    String slug;
    JlptLevel jlptLevel;
    String description;
    String thumbnailUrl;
    boolean published;
    Instant createdAt;

    // Phân quyền & tiến độ
    boolean isEnrolled;              // User hiện tại đã đăng ký chưa
    BigDecimal progressPercent;      // % hoàn thành khóa học (nếu đã đăng ký)
    Integer completedLessonsCount;   // Số bài học đã hoàn thành
    Integer totalLessons;            // Tổng số bài học

    // Danh sách mục lục các bài học (Outline)
    List<LessonOutlineResponse> lessons;
}
