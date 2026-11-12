package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonReorderRequest;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonMapper;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Sắp xếp lại bài học: danh sách client đang xem đã cũ (version lệch) → 409, không ghi gì. */
@ExtendWith(MockitoExtension.class)
class ReorderVersionCheckTest {

    @Mock CourseRepository courseRepository;
    @Mock LessonRepository lessonRepository;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock LessonMapper lessonMapper;
    @Mock LessonDraftService lessonDraftService;
    @InjectMocks LessonService lessonService;

    Lesson l1;
    Lesson l2;

    @BeforeEach
    void setUp() {
        Course course = Course.builder().title("N5").slug("n5").build();
        course.setId(1L);
        l1 = Lesson.builder().course(course).title("Bài 1").orderIndex(1).build();
        l1.setId(10L);
        l1.setVersion(3L);
        l2 = Lesson.builder().course(course).title("Bài 2").orderIndex(2).build();
        l2.setId(20L);
        l2.setVersion(8L);
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(lessonRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(new ArrayList<>(List.of(l1, l2)));
        // V66: phía người sắp xếp thấy cả 2 bài
        org.mockito.Mockito.lenient().when(lessonDraftService.currentKind())
                .thenReturn(com.jp.elearningjp.shared.enums.DraftKind.TEACHER);
        org.mockito.Mockito.lenient().when(lessonRepository.findVisibleByCourseId(1L,
                com.jp.elearningjp.shared.enums.DraftKind.TEACHER)).thenReturn(new ArrayList<>(List.of(l1, l2)));
    }

    @Test
    void hiddenLessonOfOtherSide_keepsItsSlot_visibleOnesReorderAroundIt() {
        // Bài 1.5 giáo viên mới tạo (chưa gửi duyệt) nằm giữa — ADMIN không thấy, không gửi trong danh sách
        Lesson hidden = Lesson.builder().course(l1.getCourse()).title("Bài mới").orderIndex(2).build();
        hidden.setId(15L);
        l2.setOrderIndex(3);
        when(lessonRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(new ArrayList<>(List.of(l1, hidden, l2)));
        when(lessonDraftService.currentKind()).thenReturn(com.jp.elearningjp.shared.enums.DraftKind.ADMIN);
        when(lessonRepository.findVisibleByCourseId(1L, com.jp.elearningjp.shared.enums.DraftKind.ADMIN))
                .thenReturn(new ArrayList<>(List.of(l1, l2)));

        lessonService.reorderLessons(1L, LessonReorderRequest.builder()
                .lessonIds(List.of(20L, 10L)).versions(Map.of(10L, 3L, 20L, 8L)).build());

        assertThat(l2.getOrderIndex()).isEqualTo(1);
        assertThat(hidden.getOrderIndex()).isEqualTo(2);
        assertThat(l1.getOrderIndex()).isEqualTo(3);
    }

    @Test
    void staleVersion_conflict_andOrderUntouched() {
        LessonReorderRequest request = LessonReorderRequest.builder()
                .lessonIds(List.of(20L, 10L))
                .versions(Map.of(10L, 3L, 20L, 6L)) // bài 2 đã bị người khác dời → version 8
                .build();

        assertThatThrownBy(() -> lessonService.reorderLessons(1L, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);
        assertThat(l1.getOrderIndex()).isEqualTo(1);
        assertThat(l2.getOrderIndex()).isEqualTo(2);
        verify(lessonRepository, never()).flush();
    }

    @Test
    void matchingVersions_reorders() {
        LessonReorderRequest request = LessonReorderRequest.builder()
                .lessonIds(List.of(20L, 10L))
                .versions(Map.of(10L, 3L, 20L, 8L))
                .build();

        lessonService.reorderLessons(1L, request);

        assertThat(l2.getOrderIndex()).isEqualTo(1);
        assertThat(l1.getOrderIndex()).isEqualTo(2);
    }
}
