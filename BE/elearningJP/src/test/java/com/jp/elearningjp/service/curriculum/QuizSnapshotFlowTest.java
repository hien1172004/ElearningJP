package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.QuizAnswerItem;
import com.jp.elearningjp.dto.request.curriculum.QuizSubmitRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonQuizResponse;
import com.jp.elearningjp.dto.response.curriculum.QuizSubmitResponse;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.entity.srs.SrsItem;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.QuizOptionRepository;
import com.jp.elearningjp.repository.curriculum.QuizQuestionRepository;
import com.jp.elearningjp.repository.curriculum.QuizRepository;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.repository.srs.SrsItemRepository;
import com.jp.elearningjp.repository.srs.SrsReviewLogRepository;
import com.jp.elearningjp.service.curriculum.snapshot.LessonSnapshot;
import com.jp.elearningjp.service.curriculum.snapshot.PublishedLessonResolver;
import com.jp.elearningjp.service.curriculum.snapshot.QuizSnapshot;
import com.jp.elearningjp.shared.enums.QuizQuestionType;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Học viên làm / nộp quiz theo bản xuất bản: không đọc bản đang soạn, ẩn đáp án, chấm theo snapshot. */
@ExtendWith(MockitoExtension.class)
class QuizSnapshotFlowTest {

    @Mock QuizRepository quizRepository;
    @Mock QuizQuestionRepository questionRepository;
    @Mock QuizOptionRepository optionRepository;
    @Mock LessonRepository lessonRepository;
    @Mock UserLessonProgressRepository progressRepository;
    @Mock CurrentUserService currentUserService;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock SrsItemRepository srsItemRepository;
    @Mock SrsReviewLogRepository srsReviewLogRepository;
    @Mock LessonProgressRecorder lessonProgressRecorder;
    @Mock EntityManager entityManager;
    @Mock PublishedLessonResolver publishedLessonResolver;
    @InjectMocks QuizService service;

    Lesson lesson;
    User student;
    LessonSnapshot snapshot;

    @BeforeEach
    void setUp() {
        Course course = Course.builder().title("N5").slug("n5").build();
        course.setId(1L);
        lesson = Lesson.builder().course(course).title("Bài 1").orderIndex(1).build();
        lesson.setId(10L);
        student = User.builder().email("s@test.local").build();
        student.setId(7L);

        // Bản đã duyệt: đáp án đúng là option 12; câu gắn từ vựng id 300 (SRS)
        QuizSnapshot quiz = QuizSnapshot.builder()
                .quizId(500L).title("Kiểm tra").passingScorePercent(80)
                .questions(new ArrayList<>(List.of(QuizSnapshot.Question.builder()
                        .id(1L).questionText("1+1=?").orderIndex(1)
                        .questionType(QuizQuestionType.MULTIPLE_CHOICE)
                        .explanation("Giải thích").vocabId(300L)
                        .options(new ArrayList<>(List.of(
                                QuizSnapshot.Option.builder().id(11L).optionKey((short) 1).optionText("1").correct(false).build(),
                                QuizSnapshot.Option.builder().id(12L).optionKey((short) 2).optionText("2").correct(true).build())))
                        .build())))
                .build();
        snapshot = LessonSnapshot.builder().quiz(quiz).build();
    }

    @Test
    void getLessonQuiz_fromSnapshot_hidesAnswers_andSkipsDraft() {
        when(publishedLessonResolver.forReading(10L))
                .thenReturn(Optional.of(new PublishedLessonResolver.PublishedLesson(lesson, snapshot)));

        LessonQuizResponse resp = service.getLessonQuiz(10L);

        assertThat(resp.getQuizId()).isEqualTo(500L);
        assertThat(resp.getTotalQuestions()).isEqualTo(1);
        assertThat(resp.getQuestions().get(0).getOptions()).extracting("optionText").containsExactly("1", "2");
        // DTO học viên không có trường đáp án / giải thích
        assertThat(resp.getQuestions().get(0).toString()).doesNotContain("correct").doesNotContain("Giải thích");
        verifyNoInteractions(quizRepository, questionRepository, optionRepository);
    }

    @Test
    void getLessonQuiz_lessonWithoutQuizInSnapshot_notFound() {
        when(publishedLessonResolver.forReading(10L))
                .thenReturn(Optional.of(new PublishedLessonResolver.PublishedLesson(lesson, LessonSnapshot.builder().build())));

        assertThatThrownBy(() -> service.getLessonQuiz(10L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_NOT_FOUND);
    }

    @Test
    void submitQuiz_gradesAgainstSnapshotAnswers_andSyncsSrsById() {
        when(publishedLessonResolver.forLearning(10L))
                .thenReturn(Optional.of(new PublishedLessonResolver.PublishedLesson(lesson, snapshot)));
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(lessonProgressRecorder.lockOrCreate(7L, 10L)).thenReturn(UserLessonProgress.builder().build());
        when(progressRepository.save(any(UserLessonProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        Vocabulary vocabRef = new Vocabulary();
        vocabRef.setId(300L);
        when(entityManager.getReference(Vocabulary.class, 300L)).thenReturn(vocabRef);
        when(srsItemRepository.findByUserIdAndVocabIdAndDeletedFalse(7L, 300L)).thenReturn(Optional.empty());
        when(srsItemRepository.save(any(SrsItem.class))).thenAnswer(inv -> inv.getArgument(0));

        QuizSubmitResponse resp = service.submitQuiz(10L, QuizSubmitRequest.builder()
                .answers(List.of(QuizAnswerItem.builder().questionId(1L).selectedOptionId(12L).build()))
                .build());

        assertThat(resp.getCorrectCount()).isEqualTo(1);
        assertThat(resp.isPassed()).isTrue();
        assertThat(resp.getDetails().get(0).getCorrectOptionId()).isEqualTo(12L);
        ArgumentCaptor<SrsItem> srs = ArgumentCaptor.forClass(SrsItem.class);
        verify(srsItemRepository).save(srs.capture());
        assertThat(srs.getValue().getVocab()).isSameAs(vocabRef);
        // Không đọc quiz / đáp án của bản đang soạn
        verify(quizRepository, never()).findByLessonIdAndDeletedFalse(anyLong());
        verifyNoInteractions(questionRepository, optionRepository);
    }
}
