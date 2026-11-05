package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.response.curriculum.CourseResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.mapper.CourseMapper;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.shared.response.PageResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tái hiện bug: TEACHER mở trang quản lý khóa học bị 403 vì GET /courses/admin chỉ cho ADMIN.
 * Nay TEACHER được gọi nhưng chỉ nhận khóa học do chính mình tạo.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseAdminListTest {

    private static final Long TEACHER_ID = 42L;

    @Mock CourseRepository courseRepository;
    @Mock LessonRepository lessonRepository;
    @Mock CourseEnrollmentRepository enrollmentRepository;
    @Mock CourseMapper courseMapper;
    @Mock CurrentUserService currentUserService;

    @InjectMocks CourseService courseService;

    private static Course course(long id) {
        Course c = new Course();
        c.setId(id);
        return c;
    }

    @Test
    void admin_seesAllCourses() {
        when(currentUserService.isAdmin()).thenReturn(true);
        when(courseRepository.findByDeletedFalse(any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(course(1), course(2)), inv.getArgument(0), 2));
        when(courseMapper.toCourseResponse(any(Course.class))).thenAnswer(inv -> new CourseResponse());

        PageResponse<CourseResponse> res = courseService.getAllCoursesForAdmin(1, 10);

        assertThat(res.getItems()).hasSize(2);
        verify(courseRepository, never()).findByCreatedByIdAndDeletedFalse(anyLong(), any(Pageable.class));
    }

    @Test
    void teacher_seesOnlyOwnCourses() {
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(TEACHER_ID);
        when(courseRepository.findByCreatedByIdAndDeletedFalse(eq(TEACHER_ID), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(course(7)), inv.getArgument(1), 1));
        when(courseMapper.toCourseResponse(any(Course.class))).thenAnswer(inv -> new CourseResponse());

        PageResponse<CourseResponse> res = courseService.getAllCoursesForAdmin(1, 10);

        assertThat(res.getItems()).hasSize(1);
        assertThat(res.getTotalElements()).isEqualTo(1);
        verify(courseRepository, never()).findByDeletedFalse(any(Pageable.class));
    }

    private static CourseEnrollmentRepository.CourseEnrollmentCount enrollmentCount(long courseId, long count) {
        return new CourseEnrollmentRepository.CourseEnrollmentCount() {
            @Override public Long getCourseId() { return courseId; }
            @Override public Long getEnrollmentCount() { return count; }
        };
    }

    @Test
    void list_includesTotalEnrollments_fromOneBatchQuery() {
        // Bug cũ: danh sách khóa học không có số học viên → thẻ khóa học ẩn mục này
        when(currentUserService.isAdmin()).thenReturn(true);
        when(courseRepository.findByDeletedFalse(any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(course(1), course(2)), inv.getArgument(0), 2));
        when(courseMapper.toCourseResponse(any(Course.class))).thenAnswer(inv -> new CourseResponse());
        when(enrollmentRepository.countActiveByCourseIds(List.of(1L, 2L)))
                .thenReturn(List.of(enrollmentCount(1L, 3L))); // khóa 2 chưa ai đăng ký

        PageResponse<CourseResponse> res = courseService.getAllCoursesForAdmin(1, 10);

        assertThat(res.getItems()).extracting(CourseResponse::getTotalEnrollments).containsExactly(3L, 0L);
        verify(enrollmentRepository).countActiveByCourseIds(List.of(1L, 2L));
        verify(enrollmentRepository, never()).countActiveByCourseId(anyLong());
    }
}
