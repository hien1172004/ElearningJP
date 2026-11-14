package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonProgressUpdateRequest;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.curriculum.ProgressMapper;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.QuizRepository;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.service.curriculum.snapshot.PublishedLessonResolver;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tiến độ từng phần trong bài lưu trên server: chỉ tăng, không giảm, và cần đã đăng ký khóa. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LessonSectionProgressTest {

    private static final Long LESSON_ID = 1L;
    private static final Long USER_ID = 3L;

    @Mock UserLessonProgressRepository progressRepository;
    @Mock LessonRepository lessonRepository;
    @Mock CourseRepository courseRepository;
    @Mock CourseEnrollmentRepository enrollmentRepository;
    @Mock QuizRepository quizRepository;
    @Mock ProgressMapper progressMapper;
    @Mock CurrentUserService currentUserService;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock LessonProgressRecorder progressRecorder;
    @Mock LessonDraftService lessonDraftService;
    @Mock PublishedLessonResolver publishedLessonResolver;
    @InjectMocks ProgressService progressService;

    private Lesson lesson;
    private UserLessonProgress progress;

    @BeforeEach
    void setUp() {
        lesson = mock(Lesson.class);
        when(lesson.getId()).thenReturn(LESSON_ID);
        when(lesson.getCourse()).thenReturn(mock(Course.class));
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(publishedLessonResolver.forLearning(LESSON_ID)).thenReturn(Optional.empty());
        when(lessonRepository.findByIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(progressRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        progress = UserLessonProgress.builder().status(ProgressStatus.NOT_STARTED).build();
        when(progressRecorder.lockOrCreate(USER_ID, LESSON_ID)).thenReturn(progress);
    }

    private static LessonProgressUpdateRequest viewed(LessonSectionType type, int count, Integer percent) {
        return LessonProgressUpdateRequest.builder().sectionType(type).viewedCount(count).lessonPercent(percent).build();
    }

    @Test
    void recordsViewedCountAndPercent_andStartsLesson() {
        progressService.updateSectionProgress(LESSON_ID, viewed(LessonSectionType.KANJI, 3, 20));

        assertThat(progress.getSectionProgress()).containsEntry(LessonSectionType.KANJI, 3);
        assertThat(progress.getProgressPercent()).isEqualTo(20);
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
    }

    @Test
    void smallerValues_doNotOverwrite() {
        progressService.updateSectionProgress(LESSON_ID, viewed(LessonSectionType.KANJI, 5, 40));
        progressService.updateSectionProgress(LESSON_ID, viewed(LessonSectionType.KANJI, 2, 10));

        assertThat(progress.getSectionProgress()).containsEntry(LessonSectionType.KANJI, 5);
        assertThat(progress.getProgressPercent()).isEqualTo(40);
    }

    @Test
    void otherSections_keptWhenUpdatingOne() {
        progressService.updateSectionProgress(LESSON_ID, viewed(LessonSectionType.KANJI, 4, null));
        progressService.updateSectionProgress(LESSON_ID, viewed(LessonSectionType.VOCABULARY, 2, null));

        assertThat(progress.getSectionProgress())
                .containsEntry(LessonSectionType.KANJI, 4)
                .containsEntry(LessonSectionType.VOCABULARY, 2);
    }

    @Test
    void notEnrolled_rejectedWithoutSaving() {
        doThrow(new AppException(ErrorCode.UNAUTHORIZED)).when(courseAccessGuard).requireEnrolledInLesson(lesson);

        assertThatThrownBy(() -> progressService.updateSectionProgress(LESSON_ID, viewed(LessonSectionType.KANJI, 1, 5)))
                .isInstanceOf(AppException.class);
        verify(progressRepository, never()).save(any());
    }
}
