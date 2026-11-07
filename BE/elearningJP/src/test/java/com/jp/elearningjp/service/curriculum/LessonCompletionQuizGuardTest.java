package com.jp.elearningjp.service.curriculum;

import static org.mockito.ArgumentMatchers.eq;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.Quiz;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Bài có quiz chỉ hoàn thành được khi bestScore do server chấm đã đạt điểm qua — ở cả 2 route. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LessonCompletionQuizGuardTest {

    private static final Long LESSON_ID = 1L;
    private static final Long COURSE_ID = 2L;
    private static final Long USER_ID = 3L;

    @Mock UserLessonProgressRepository progressRepository;
    @Mock LessonRepository lessonRepository;
    @Mock CourseRepository courseRepository;
    @Mock CourseEnrollmentRepository enrollmentRepository;
    @Mock QuizRepository quizRepository;
    @Mock ProgressMapper progressMapper;
    @Mock CurrentUserService currentUserService;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock EnrollmentService enrollmentService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock LessonProgressRecorder progressRecorder;
    @Mock LessonSpeakingService lessonSpeakingService;

    @InjectMocks ProgressService progressService;
    @InjectMocks LessonService lessonService;

    @BeforeEach
    void setUp() {
        Course course = mock(Course.class);
        when(course.getId()).thenReturn(COURSE_ID);
        Lesson lesson = mock(Lesson.class);
        when(lesson.getCourse()).thenReturn(course);
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);

        when(lessonRepository.findByIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(progressRepository.calculateCourseProgressPercent(USER_ID, COURSE_ID)).thenReturn(50);
        when(progressRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(progressRecorder.lockOrCreate(USER_ID, LESSON_ID))
                .thenReturn(UserLessonProgress.builder().status(ProgressStatus.NOT_STARTED).build());
    }

    private void lessonHasQuizPassingAt(int percent) {
        Quiz quiz = mock(Quiz.class);
        when(quiz.getPassingScorePercent()).thenReturn(percent);
        when(quizRepository.findByLessonIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.of(quiz));
    }

    private void existingProgress(String bestScore) {
        UserLessonProgress progress = UserLessonProgress.builder()
                .status(ProgressStatus.IN_PROGRESS)
                .bestScore(bestScore == null ? null : new BigDecimal(bestScore))
                .build();
        when(progressRecorder.lockOrCreate(USER_ID, LESSON_ID)).thenReturn(progress);
    }

    private void assertRejected(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_COMPLETE_LESSON);
        verify(progressRepository, never()).save(any());
    }

    @Test
    void progressRoute_quizNeverSubmitted_rejected() {
        lessonHasQuizPassingAt(80);
        assertRejected(() -> progressService.completeLesson(LESSON_ID));
    }

    @Test
    void progressRoute_bestScoreBelowPassing_rejected() {
        lessonHasQuizPassingAt(80);
        existingProgress("79.9");
        assertRejected(() -> progressService.completeLesson(LESSON_ID));
    }

    @Test
    void progressRoute_bestScoreAtPassing_completes() {
        lessonHasQuizPassingAt(80);
        existingProgress("80.0");

        progressService.completeLesson(LESSON_ID);

        verify(progressRepository).save(any());
        verify(progressRecorder).onLessonCompleted(eq(USER_ID), any(), any(), eq(true));
    }

    @Test
    void progressRoute_noQuiz_completes() {
        when(quizRepository.findByLessonIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.empty());

        progressService.completeLesson(LESSON_ID);

        verify(progressRepository).save(any());
    }

    @Test
    void lessonRoute_quizNeverSubmitted_rejected() {
        lessonHasQuizPassingAt(80);
        assertRejected(() -> lessonService.completeLesson(LESSON_ID));
    }

    @Test
    void lessonRoute_bestScoreBelowPassing_rejected() {
        lessonHasQuizPassingAt(80);
        existingProgress("50.0");
        assertRejected(() -> lessonService.completeLesson(LESSON_ID));
    }

    @Test
    void lessonRoute_bestScoreAtPassing_completes() {
        lessonHasQuizPassingAt(80);
        existingProgress("80.0");

        assertThat(lessonService.completeLesson(LESSON_ID)).isNotNull();

        verify(progressRepository).save(any());
        verify(progressRecorder).onLessonCompleted(eq(USER_ID), any(), any(), eq(true));
    }

    @Test
    void lessonRoute_alreadyCompleted_doesNotFireCompletionAgain() {
        // Bấm hoàn thành lần 2 không được cộng XP lần nữa.
        UserLessonProgress done = UserLessonProgress.builder().status(ProgressStatus.COMPLETED).build();
        when(progressRecorder.lockOrCreate(USER_ID, LESSON_ID)).thenReturn(done);
        when(quizRepository.findByLessonIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.empty());

        assertThat(lessonService.completeLesson(LESSON_ID)).isNotNull();

        verify(progressRecorder, never()).onLessonCompleted(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void lessonRoute_noQuiz_completes() {
        when(quizRepository.findByLessonIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.empty());

        assertThat(lessonService.completeLesson(LESSON_ID)).isNotNull();

        verify(progressRepository).save(any());
    }
}
