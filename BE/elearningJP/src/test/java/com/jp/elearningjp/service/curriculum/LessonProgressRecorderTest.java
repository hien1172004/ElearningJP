package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.shared.event.curriculum.LessonCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Hoàn thành bài phải phát event XP và tự hoàn thành khóa khi đạt 100%. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LessonProgressRecorderTest {

    private static final Long USER_ID = 1L;
    private static final Long COURSE_ID = 10L;

    @Mock UserLessonProgressRepository progressRepository;
    @Mock EnrollmentService enrollmentService;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks LessonProgressRecorder recorder;

    private Lesson lesson;

    @BeforeEach
    void setUp() {
        Course course = new Course();
        course.setId(COURSE_ID);
        lesson = Lesson.builder().course(course).build();
        lesson.setId(20L);
    }

    @Test
    void lastLesson_publishesEventAndCompletesCourse() {
        when(progressRepository.calculateCourseProgressPercent(USER_ID, COURSE_ID)).thenReturn(100);

        assertThat(recorder.onLessonCompleted(USER_ID, lesson, null, false)).isEqualTo(100);

        // Phải flush trước khi tính % bằng native SQL, nếu không bài vừa hoàn thành chưa được đếm.
        var order = inOrder(progressRepository);
        order.verify(progressRepository).flush();
        order.verify(progressRepository).calculateCourseProgressPercent(USER_ID, COURSE_ID);
        verify(eventPublisher).publishEvent(any(LessonCompletedEvent.class));
        verify(enrollmentService).markAsCompletedIfFull(USER_ID, COURSE_ID);
    }

    @Test
    void notLastLesson_publishesEventOnly() {
        when(progressRepository.calculateCourseProgressPercent(USER_ID, COURSE_ID)).thenReturn(50);

        recorder.onLessonCompleted(USER_ID, lesson, null, false);

        verify(eventPublisher).publishEvent(any(LessonCompletedEvent.class));
        verify(enrollmentService, never()).markAsCompletedIfFull(anyLong(), anyLong());
    }
}
