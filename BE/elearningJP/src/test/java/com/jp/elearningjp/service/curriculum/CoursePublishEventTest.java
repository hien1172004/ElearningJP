package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.response.curriculum.CourseResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.mapper.CourseMapper;
import com.jp.elearningjp.mapper.LessonMapper;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.QuizRepository;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.event.curriculum.CoursePublishStatusChangedEvent;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link CourseService#publishCourse} phát event để báo học viên — chỉ khi trạng thái thật sự đổi. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoursePublishEventTest {

    @Mock CourseRepository courseRepository;
    @Mock LessonRepository lessonRepository;
    @Mock CourseEnrollmentRepository enrollmentRepository;
    @Mock UserLessonProgressRepository progressRepository;
    @Mock QuizRepository quizRepository;
    @Mock UserRepository userRepository;
    @Mock CourseMapper courseMapper;
    @Mock LessonMapper lessonMapper;
    @Mock CurrentUserService currentUserService;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks CourseService courseService;

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(10L);
        course.setTitle("JLPT N5");
        when(courseRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        when(courseMapper.toCourseResponse(any(Course.class))).thenReturn(new CourseResponse());
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void draftToPublished_publishesEvent() {
        course.setPublished(false);

        courseService.publishCourse(10L, true);

        ArgumentCaptor<CoursePublishStatusChangedEvent> captor =
                ArgumentCaptor.forClass(CoursePublishStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getCourseId()).isEqualTo(10L);
        assertThat(captor.getValue().getCourseTitle()).isEqualTo("JLPT N5");
        assertThat(captor.getValue().isPublished()).isTrue();
        assertThat(captor.getValue().getChangedByUserId()).isEqualTo(1L);
    }

    @Test
    void publishedToDraft_publishesEvent() {
        course.setPublished(true);

        courseService.publishCourse(10L, false);

        ArgumentCaptor<CoursePublishStatusChangedEvent> captor =
                ArgumentCaptor.forClass(CoursePublishStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().isPublished()).isFalse();
    }

    @Test
    void sameStatus_doesNotPublishEvent() {
        course.setPublished(true);

        courseService.publishCourse(10L, true);

        verify(eventPublisher, never()).publishEvent(any());
    }
}
