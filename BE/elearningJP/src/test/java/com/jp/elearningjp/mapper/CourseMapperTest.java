package com.jp.elearningjp.mapper;

import com.jp.elearningjp.dto.request.curriculum.CourseUpdateRequest;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CourseMapperTest {

    private final CourseMapper mapper = new CourseMapperImpl();

    @ParameterizedTest(name = "published ban đầu = {0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("Sửa thông tin khóa học giữ nguyên trạng thái xuất bản trước khi sửa")
    void updateCourseFromRequest_keepsPublishedStatus(boolean initiallyPublished) {
        Course course = Course.builder()
                .title("Cũ")
                .jlptLevel(JlptLevel.N5)
                .published(initiallyPublished)
                .build();

        CourseUpdateRequest request = CourseUpdateRequest.builder()
                .title("Mới")
                .jlptLevel(JlptLevel.N4)
                .description("Mô tả mới")
                .build();

        mapper.updateCourseFromRequest(request, course);

        assertThat(course.getPublished()).isEqualTo(initiallyPublished);
        assertThat(course.getTitle()).isEqualTo("Mới");
        assertThat(course.getJlptLevel()).isEqualTo(JlptLevel.N4);
        assertThat(course.getDescription()).isEqualTo("Mô tả mới");
    }
}
