package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.QuizAnswerItem;
import com.jp.elearningjp.dto.request.curriculum.QuizSubmitRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonQuizResponse;
import com.jp.elearningjp.dto.response.curriculum.QuizDetailItemResponse;
import com.jp.elearningjp.dto.response.curriculum.QuizSubmitResponse;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.Quiz;
import com.jp.elearningjp.entity.curriculum.QuizOption;
import com.jp.elearningjp.entity.curriculum.QuizQuestion;
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
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.enums.QuizQuestionType;
import com.jp.elearningjp.shared.enums.SrsStatus;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link QuizService} — focus vào logic nghiệp vụ cốt lõi:
 *
 * <ul>
 *   <li>Grading 3 dạng câu hỏi (MULTIPLE_CHOICE, FILL_IN_THE_BLANK, MATCHING)</li>
 *   <li>Progress update (bestScore, status COMPLETED, completedAt)</li>
 *   <li>SRS integration (syncQuestionResultToSrs)</li>
 *   <li>Validate section TEST, quiz tồn tại</li>
 *   <li>Ẩn đáp án đúng khi build response</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock QuizRepository quizRepository;
    @Mock QuizQuestionRepository questionRepository;
    @Mock QuizOptionRepository optionRepository;
    @Mock LessonRepository lessonRepository;
    @Mock UserLessonProgressRepository progressRepository;
    @Mock CurrentUserService currentUserService;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock SrsItemRepository srsItemRepository;
    @Mock SrsReviewLogRepository srsReviewLogRepository;

    @InjectMocks QuizService service;

    private Course course;
    private Lesson lesson;
    private Quiz quiz;
    private User user;

    @BeforeEach
    void setUp() {
        course = Course.builder().title("N5").slug("n5").build();
        course.setId(1L);

        lesson = Lesson.builder().course(course).title("Bài 1").orderIndex(1).build();
        lesson.setId(10L);

        quiz = Quiz.builder()
                .lesson(lesson)
                .title("Bài kiểm tra: Bài 1")
                .passingScorePercent(80)
                .questions(new ArrayList<>())
                .build();
        quiz.setId(500L);

        user = User.builder().email("stu@test.com").build();
        user.setId(7L);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private QuizOption option(Long id, Short key, String text, boolean correct) {
        QuizOption opt = QuizOption.builder()
                .question(null) // sẽ set sau nếu cần
                .optionKey(key)
                .optionText(text)
                .correct(correct)
                .build();
        opt.setId(id);
        return opt;
    }

    private QuizOption matchingOption(Long id, String matchKey, String text) {
        QuizOption opt = QuizOption.builder()
                .optionKey((short) id.intValue())
                .optionText(text)
                .matchKey(matchKey)
                .build();
        opt.setId(id);
        return opt;
    }

    private QuizQuestion question(Long id, QuizQuestionType type, String text) {
        QuizQuestion q = QuizQuestion.builder()
                .quiz(quiz)
                .questionText(text)
                .questionType(type)
                .orderIndex(id.intValue())
                .options(new ArrayList<>())
                .build();
        q.setId(id);
        return q;
    }

    // =====================================================================
    // getLessonQuiz
    // =====================================================================

    @Nested
    @DisplayName("getLessonQuiz")
    class GetLessonQuiz {

        @Test
        @DisplayName("Trả về quiz ẩn hoàn toàn đáp án đúng (isCorrect)")
        void returnsQuiz_withoutIsCorrect() {
            QuizQuestion q1 = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "1+1=?");
            QuizOption optCorrect = option(11L, (short) 1, "2", true);
            optCorrect.setQuestion(q1);
            q1.getOptions().add(optCorrect);

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q1));

            LessonQuizResponse resp = service.getLessonQuiz(10L);

            assertThat(resp.getQuizId()).isEqualTo(500L);
            assertThat(resp.getTotalQuestions()).isEqualTo(1);
            assertThat(resp.getQuestions()).hasSize(1);
            assertThat(resp.getQuestions().get(0).getOptions()).hasSize(1);
            // Đáp án đúng phải bị ẩn (response chỉ chứa id/key/text, không isCorrect)
            assertThat(resp.getQuestions().get(0).getOptions().get(0).getId()).isEqualTo(11L);
            assertThat(resp.getQuestions().get(0).getOptions().get(0).getOptionText()).isEqualTo("2");
        }

        @Test
        @DisplayName("Lesson không tồn tại → LESSON_NOT_FOUND")
        void lessonNotFound_throws() {
            when(lessonRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLessonQuiz(99L))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LESSON_NOT_FOUND);
        }

        @Test
        @DisplayName("Lesson chưa có quiz → QUIZ_NOT_FOUND")
        void noQuiz_throws() {
            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLessonQuiz(10L))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.QUIZ_NOT_FOUND);
        }
    }

    // =====================================================================
    // submitQuiz - MULTIPLE_CHOICE
    // =====================================================================

    @Nested
    @DisplayName("submitQuiz - MULTIPLE_CHOICE")
    class SubmitMultipleChoice {

        @Test
        @DisplayName("Chọn đúng 1/1 câu → 100% → pass → status COMPLETED")
        void allCorrect_passAndComplete() {
            QuizQuestion q = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "1+1=?");
            QuizOption optA = option(11L, (short) 1, "1", false);
            QuizOption optB = option(12L, (short) 2, "2", true);
            optA.setQuestion(q); optB.setQuestion(q);
            q.getOptions().add(optA); q.getOptions().add(optB);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).selectedOptionId(12L).responseTimeMs(2000).build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(optA, optB));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any(UserLessonProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(srsItemRepository.findByUserIdAndVocabIdAndDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.empty());
            when(srsItemRepository.findByUserIdAndCharacterIdAndDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.empty());
            when(srsItemRepository.save(any(SrsItem.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(srsReviewLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitResponse resp = service.submitQuiz(10L, req);

            assertThat(resp.getTotalQuestions()).isEqualTo(1);
            assertThat(resp.getCorrectCount()).isEqualTo(1);
            assertThat(resp.getScorePercent()).isEqualByComparingTo(new BigDecimal("100.0"));
            assertThat(resp.isPassed()).isTrue();
            assertThat(resp.getLessonStatus()).isEqualTo(ProgressStatus.COMPLETED);
            assertThat(resp.getBestScore()).isEqualByComparingTo(new BigDecimal("100.0"));
            assertThat(resp.getDetails()).hasSize(1);
            assertThat(resp.getDetails().get(0).isCorrect()).isTrue();
            assertThat(resp.getDetails().get(0).getSelectedOptionId()).isEqualTo(12L);
            assertThat(resp.getDetails().get(0).getCorrectOptionId()).isEqualTo(12L);
        }

        @Test
        @DisplayName("Chọn sai → 0% → fail → status giữ nguyên (không COMPLETED)")
        void allWrong_fail() {
            QuizQuestion q = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "1+1=?");
            QuizOption optA = option(11L, (short) 1, "1", false);
            QuizOption optB = option(12L, (short) 2, "2", true);
            optA.setQuestion(q); optB.setQuestion(q);
            q.getOptions().add(optA); q.getOptions().add(optB);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).selectedOptionId(11L).build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(optA, optB));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.of(UserLessonProgress.builder()
                            .user(user).lesson(lesson)
                            .status(ProgressStatus.IN_PROGRESS)
                            .build()));
            when(progressRepository.save(any(UserLessonProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitResponse resp = service.submitQuiz(10L, req);

            assertThat(resp.getCorrectCount()).isZero();
            assertThat(resp.getScorePercent()).isEqualByComparingTo(new BigDecimal("0.0"));
            assertThat(resp.isPassed()).isFalse();
            assertThat(resp.getLessonStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Không chọn option nào → fail, selectedOptionText='Chưa chọn'")
        void notSelected() {
            QuizQuestion q = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "1+1=?");
            QuizOption optA = option(11L, (short) 1, "1", false);
            QuizOption optB = option(12L, (short) 2, "2", true);
            optA.setQuestion(q); optB.setQuestion(q);
            q.getOptions().add(optA); q.getOptions().add(optB);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).selectedOptionId(null).build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(optA, optB));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitResponse resp = service.submitQuiz(10L, req);

            assertThat(resp.getDetails().get(0).isCorrect()).isFalse();
            assertThat(resp.getDetails().get(0).getSelectedOptionText()).isEqualTo("Chưa chọn");
        }
    }

    // =====================================================================
    // submitQuiz - FILL_IN_THE_BLANK
    // =====================================================================

    @Nested
    @DisplayName("submitQuiz - FILL_IN_THE_BLANK")
    class SubmitFillInBlank {

        @Test
        @DisplayName("Đáp án đúng (case-insensitive, trim) → pass")
        void correctText_caseInsensitive() {
            QuizQuestion q = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionText("学生 means?")
                    .questionType(QuizQuestionType.FILL_IN_THE_BLANK)
                    .correctTextAnswer("học sinh")
                    .orderIndex(1)
                    .build();
            q.setId(1L);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).textAnswer("  HỌC SINH  ").build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitResponse resp = service.submitQuiz(10L, req);

            assertThat(resp.getCorrectCount()).isEqualTo(1);
            assertThat(resp.getDetails().get(0).isCorrect()).isTrue();
            assertThat(resp.getDetails().get(0).getUserTextAnswer()).isEqualTo("HỌC SINH");
        }

        @Test
        @DisplayName("Đáp án sai → fail")
        void wrongText() {
            QuizQuestion q = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionText("学生 means?")
                    .questionType(QuizQuestionType.FILL_IN_THE_BLANK)
                    .correctTextAnswer("học sinh")
                    .orderIndex(1)
                    .build();
            q.setId(1L);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).textAnswer("sinh viên").build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitResponse resp = service.submitQuiz(10L, req);

            assertThat(resp.getCorrectCount()).isZero();
            assertThat(resp.getDetails().get(0).isCorrect()).isFalse();
        }

        @Test
        @DisplayName("Câu hỏi không có correctTextAnswer → luôn fail (tránh answer cheating)")
        void noCorrectTextAnswer_alwaysFails() {
            QuizQuestion q = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionText("Lỗi cấu hình")
                    .questionType(QuizQuestionType.FILL_IN_THE_BLANK)
                    .correctTextAnswer(null)
                    .orderIndex(1)
                    .build();
            q.setId(1L);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).textAnswer("anything").build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitResponse resp = service.submitQuiz(10L, req);

            assertThat(resp.getDetails().get(0).isCorrect()).isFalse();
        }
    }

    // =====================================================================
    // submitQuiz - MATCHING
    // =====================================================================

    @Nested
    @DisplayName("submitQuiz - MATCHING")
    class SubmitMatching {

        @Test
        @DisplayName("Ghép đúng 100% → pass")
        void allMatchedCorrect_pass() {
            QuizQuestion q = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionText("Ghép cặp")
                    .questionType(QuizQuestionType.MATCHING)
                    .orderIndex(1)
                    .build();
            q.setId(1L);

            QuizOption o1 = matchingOption(11L, "A", "Apple");
            QuizOption o2 = matchingOption(12L, "B", "Banana");
            o1.setQuestion(q); o2.setQuestion(q);
            q.getOptions().add(o1); q.getOptions().add(o2);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L)
                            .matchingAnswers(Map.of("A", "Apple", "B", "Banana"))
                            .build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(o1, o2));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitResponse resp = service.submitQuiz(10L, req);

            assertThat(resp.getCorrectCount()).isEqualTo(1);
            assertThat(resp.getDetails().get(0).getAccuracyPercent()).isEqualTo(100.0);
            assertThat(resp.getDetails().get(0).isCorrect()).isTrue();
            assertThat(resp.getDetails().get(0).getCorrectMatchingAnswers())
                    .containsEntry("A", "Apple").containsEntry("B", "Banana");
        }

        @Test
        @DisplayName("Ghép 1/2 đúng → 50% → fail (yêu cầu 100% mới pass)")
        void halfMatched_fail() {
            QuizQuestion q = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionText("Ghép cặp")
                    .questionType(QuizQuestionType.MATCHING)
                    .orderIndex(1)
                    .build();
            q.setId(1L);

            QuizOption o1 = matchingOption(11L, "A", "Apple");
            QuizOption o2 = matchingOption(12L, "B", "Banana");
            o1.setQuestion(q); o2.setQuestion(q);
            q.getOptions().add(o1); q.getOptions().add(o2);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L)
                            .matchingAnswers(Map.of("A", "Apple", "B", "Apple")) // B sai
                            .build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(o1, o2));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitResponse resp = service.submitQuiz(10L, req);

            assertThat(resp.getCorrectCount()).isZero();
            assertThat(resp.getDetails().get(0).getAccuracyPercent()).isEqualTo(50.0);
            assertThat(resp.getDetails().get(0).isCorrect()).isFalse();
        }

        @Test
        @DisplayName("Không gửi matchingAnswers (empty) → 0% → fail")
        void noMatchingAnswers_fail() {
            QuizQuestion q = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionType(QuizQuestionType.MATCHING)
                    .orderIndex(1)
                    .build();
            q.setId(1L);

            QuizOption o1 = matchingOption(11L, "A", "Apple");
            o1.setQuestion(q);
            q.getOptions().add(o1);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder().questionId(1L).build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(o1));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitResponse resp = service.submitQuiz(10L, req);

            assertThat(resp.getDetails().get(0).isCorrect()).isFalse();
            assertThat(resp.getDetails().get(0).getAccuracyPercent()).isEqualTo(0.0);
        }
    }

    // =====================================================================
    // submitQuiz - Progress & bestScore
    // =====================================================================

    @Nested
    @DisplayName("submitQuiz - Progress tracking")
    class ProgressTracking {

        @Test
        @DisplayName("bestScore mới cao hơn → cập nhật bestScore")
        void bestScore_updated() {
            QuizQuestion q = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "?");
            QuizOption optA = option(11L, (short) 1, "A", true);
            optA.setQuestion(q);
            q.getOptions().add(optA);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).selectedOptionId(11L).build()))
                    .build();

            UserLessonProgress existing = UserLessonProgress.builder()
                    .user(user).lesson(lesson)
                    .bestScore(new BigDecimal("60.0"))
                    .status(ProgressStatus.IN_PROGRESS)
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(optA));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.of(existing));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitResponse resp = service.submitQuiz(10L, req);

            assertThat(resp.getBestScore()).isEqualByComparingTo(new BigDecimal("100.0"));
            assertThat(existing.getStatus()).isEqualTo(ProgressStatus.COMPLETED);
        }

        @Test
        @DisplayName("bestScore mới thấp hơn → giữ bestScore cũ")
        void bestScore_kept() {
            QuizQuestion q = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "?");
            QuizOption optA = option(11L, (short) 1, "A", true);
            optA.setQuestion(q);
            q.getOptions().add(optA);

            // Trả lời sai → score = 0
            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).selectedOptionId(99L).build())) // option không tồn tại
                    .build();

            UserLessonProgress existing = UserLessonProgress.builder()
                    .user(user).lesson(lesson)
                    .bestScore(new BigDecimal("90.0"))
                    .status(ProgressStatus.COMPLETED)
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(optA));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.of(existing));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitResponse resp = service.submitQuiz(10L, req);

            assertThat(resp.getBestScore()).isEqualByComparingTo(new BigDecimal("90.0"));
        }

        @Test
        @DisplayName("Pass → set completedAt (chỉ set 1 lần đầu)")
        void pass_setsCompletedAt_once() {
            QuizQuestion q = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "?");
            QuizOption optA = option(11L, (short) 1, "A", true);
            optA.setQuestion(q);
            q.getOptions().add(optA);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).selectedOptionId(11L).build()))
                    .build();

            // existing đã có completedAt
            UserLessonProgress existing = UserLessonProgress.builder()
                    .user(user).lesson(lesson)
                    .bestScore(new BigDecimal("100.0"))
                    .status(ProgressStatus.COMPLETED)
                    .completedAt(java.time.Instant.parse("2024-01-01T00:00:00Z"))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(optA));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.of(existing));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.submitQuiz(10L, req);

            // Không bị override về Instant.now()
            assertThat(existing.getCompletedAt())
                    .isEqualTo(java.time.Instant.parse("2024-01-01T00:00:00Z"));
        }
    }

    // =====================================================================
    // submitQuiz - SRS Integration
    // =====================================================================

    @Nested
    @DisplayName("submitQuiz - SRS integration")
    class SrsIntegration {

        @Test
        @DisplayName("Câu hỏi có vocab + đúng → tạo SrsItem mới với status LEARNING")
        void vocabCorrect_createsSrsItem() {
            Vocabulary vocab = Vocabulary.builder().word("学校").hiragana("がっこう").build();
            vocab.setId(200L);

            QuizQuestion q = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "学校 means?");
            q.setVocab(vocab);
            QuizOption optA = option(11L, (short) 1, "trường học", true);
            optA.setQuestion(q);
            q.getOptions().add(optA);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).selectedOptionId(11L).responseTimeMs(2000).build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(optA));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(srsItemRepository.findByUserIdAndVocabIdAndDeletedFalse(7L, 200L))
                    .thenReturn(Optional.empty());
            when(srsItemRepository.save(any(SrsItem.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(srsReviewLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.submitQuiz(10L, req);

            ArgumentCaptor<SrsItem> srsCaptor = ArgumentCaptor.forClass(SrsItem.class);
            verify(srsItemRepository, times(1)).save(srsCaptor.capture());
            SrsItem saved = srsCaptor.getValue();
            assertThat(saved.getVocab().getId()).isEqualTo(200L);
            assertThat(saved.getEaseFactor()).isEqualByComparingTo(new BigDecimal("2.50"));
            assertThat(saved.getStatus()).isEqualTo(SrsStatus.LEARNING);
            // EASY (2s < 3s) → interval=3, status=REVIEWING
            assertThat(saved.getIntervalDays()).isEqualTo(3);
            assertThat(saved.getStatus()).isEqualTo(SrsStatus.REVIEWING);
            verify(srsReviewLogRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Câu hỏi có vocab + sai → AGAIN, lapseCount tăng")
        void vocabWrong_incrementsLapse() {
            Vocabulary vocab = Vocabulary.builder().word("学校").build();
            vocab.setId(200L);

            SrsItem existingSrs = SrsItem.builder()
                    .user(user).vocab(vocab)
                    .easeFactor(new BigDecimal("2.50"))
                    .intervalDays(5)
                    .repetitionCount(2)
                    .lapseCount(0)
                    .status(SrsStatus.REVIEWING)
                    .build();
            existingSrs.setId(900L);

            QuizQuestion q = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "?");
            q.setVocab(vocab);
            QuizOption optA = option(11L, (short) 1, "A", false);
            QuizOption optB = option(12L, (short) 2, "B", true);
            optA.setQuestion(q); optB.setQuestion(q);
            q.getOptions().add(optA); q.getOptions().add(optB);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).selectedOptionId(11L).build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(optA, optB));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(srsItemRepository.findByUserIdAndVocabIdAndDeletedFalse(7L, 200L))
                    .thenReturn(Optional.of(existingSrs));
            when(srsItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(srsReviewLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.submitQuiz(10L, req);

            ArgumentCaptor<SrsItem> srsCaptor = ArgumentCaptor.forClass(SrsItem.class);
            verify(srsItemRepository).save(srsCaptor.capture());
            SrsItem saved = srsCaptor.getValue();
            // AGAIN: lapseCount=1, repetition=0, interval=1, status=LEARNING
            assertThat(saved.getLapseCount()).isEqualTo(1);
            assertThat(saved.getRepetitionCount()).isZero();
            assertThat(saved.getIntervalDays()).isEqualTo(1);
            assertThat(saved.getStatus()).isEqualTo(SrsStatus.LEARNING);
        }

        @Test
        @DisplayName("Câu hỏi có character (kanji) → tạo/cập nhật SrsItem")
        void kanjiCharacter_syncsSrs() {
            WritingCharacter character = WritingCharacter.builder().character("学").build();
            character.setId(300L);

            QuizQuestion q = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "?");
            q.setCharacter(character);
            QuizOption optA = option(11L, (short) 1, "học", true);
            optA.setQuestion(q);
            q.getOptions().add(optA);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).selectedOptionId(11L).responseTimeMs(20000).build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(optA));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(srsItemRepository.findByUserIdAndCharacterIdAndDeletedFalse(7L, 300L))
                    .thenReturn(Optional.empty());
            when(srsItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(srsReviewLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.submitQuiz(10L, req);

            ArgumentCaptor<SrsItem> srsCaptor = ArgumentCaptor.forClass(SrsItem.class);
            verify(srsItemRepository).save(srsCaptor.capture());
            SrsItem saved = srsCaptor.getValue();
            assertThat(saved.getCharacter().getId()).isEqualTo(300L);
            // 20s > 15s → HARD
            // First review, HARD, newInterval = max(1, currentInterval=0 * 1.2) = 1
            assertThat(saved.getStatus()).isEqualTo(SrsStatus.LEARNING);
        }

        @Test
        @DisplayName("Câu hỏi KHÔNG có vocab/character → KHÔNG sync SRS")
        void noVocabOrCharacter_skipSrs() {
            QuizQuestion q = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "?");
            QuizOption optA = option(11L, (short) 1, "A", true);
            optA.setQuestion(q);
            q.getOptions().add(optA);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).selectedOptionId(11L).build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(optA));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.submitQuiz(10L, req);

            verify(srsItemRepository, never()).save(any());
            verify(srsReviewLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("MATCHING accuracy 50% → HARD rating")
        void matchingPartialCorrect_hardRating() {
            Vocabulary vocab = Vocabulary.builder().word("A").build();
            vocab.setId(200L);

            SrsItem existingSrs = SrsItem.builder()
                    .user(user).vocab(vocab)
                    .easeFactor(new BigDecimal("2.50"))
                    .intervalDays(5)
                    .repetitionCount(2)
                    .lapseCount(0)
                    .status(SrsStatus.REVIEWING)
                    .build();
            existingSrs.setId(900L);

            QuizQuestion q = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionType(QuizQuestionType.MATCHING)
                    .build();
            q.setId(1L);
            q.setVocab(vocab);

            QuizOption o1 = matchingOption(11L, "A", "Apple");
            QuizOption o2 = matchingOption(12L, "B", "Banana");
            o1.setQuestion(q); o2.setQuestion(q);
            q.getOptions().add(o1); q.getOptions().add(o2);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L)
                            .matchingAnswers(Map.of("A", "Apple")) // B bỏ trống → 50%
                            .build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(o1, o2));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(srsItemRepository.findByUserIdAndVocabIdAndDeletedFalse(7L, 200L))
                    .thenReturn(Optional.of(existingSrs));
            when(srsItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(srsReviewLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.submitQuiz(10L, req);

            ArgumentCaptor<SrsItem> srsCaptor = ArgumentCaptor.forClass(SrsItem.class);
            verify(srsItemRepository).save(srsCaptor.capture());
            SrsItem saved = srsCaptor.getValue();
            // 50% → HARD (60-99% range) → repetition tăng 1, lapse không tăng
            assertThat(saved.getRepetitionCount()).isEqualTo(3);
            assertThat(saved.getLapseCount()).isZero();
        }
    }

    // =====================================================================
    // submitQuiz - mixed types
    // =====================================================================

    @Nested
    @DisplayName("submitQuiz - mixed question types")
    class MixedTypes {

        @Test
        @DisplayName("Quiz 4 câu (2 MC + 1 FIB + 1 MATCH), trả lời đúng 3 → 75% → fail")
        void mixed_3Of4_correct_failBelow80() {
            // Q1: MC đúng
            QuizQuestion q1 = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "Q1");
            QuizOption q1a = option(11L, (short) 1, "A", true);
            q1a.setQuestion(q1);
            q1.getOptions().add(q1a);

            // Q2: MC sai
            QuizQuestion q2 = question(2L, QuizQuestionType.MULTIPLE_CHOICE, "Q2");
            QuizOption q2a = option(21L, (short) 1, "A", true);
            q2a.setQuestion(q2);
            q2.getOptions().add(q2a);

            // Q3: FIB đúng
            QuizQuestion q3 = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionType(QuizQuestionType.FILL_IN_THE_BLANK)
                    .correctTextAnswer("tokyo")
                    .orderIndex(3)
                    .build();
            q3.setId(3L);

            // Q4: MATCHING 100% đúng
            QuizQuestion q4 = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionType(QuizQuestionType.MATCHING)
                    .orderIndex(4)
                    .build();
            q4.setId(4L);
            QuizOption q4o1 = matchingOption(41L, "X", "X-val");
            q4o1.setQuestion(q4);
            q4.getOptions().add(q4o1);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(
                            QuizAnswerItem.builder().questionId(1L).selectedOptionId(11L).build(), // ✓
                            QuizAnswerItem.builder().questionId(2L).selectedOptionId(99L).build(), // ✗
                            QuizAnswerItem.builder().questionId(3L).textAnswer("Tokyo").build(),   // ✓
                            QuizAnswerItem.builder().questionId(4L)
                                    .matchingAnswers(Map.of("X", "X-val")).build()              // ✓
                    ))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q1, q2, q3, q4));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(q1a));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(2L))
                    .thenReturn(List.of(q2a));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(4L))
                    .thenReturn(List.of(q4o1));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitResponse resp = service.submitQuiz(10L, req);

            assertThat(resp.getTotalQuestions()).isEqualTo(4);
            assertThat(resp.getCorrectCount()).isEqualTo(3);
            assertThat(resp.getScorePercent()).isEqualByComparingTo(new BigDecimal("75.0"));
            assertThat(resp.isPassed()).isFalse(); // 75% < 80%
        }

        @Test
        @DisplayName("Quiz 0 câu hỏi → QUIZ_NOT_FOUND")
        void emptyQuiz_throws() {
            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of())
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.submitQuiz(10L, req))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.QUIZ_NOT_FOUND);
        }
    }

    // =====================================================================
    // deleteQuestion
    // =====================================================================

    @Nested
    @DisplayName("deleteQuestion")
    class DeleteQuestion {

        @Test
        @DisplayName("Xoá mềm câu hỏi tồn tại")
        void delete_existing_softDeletes() {
            QuizQuestion q = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "?");
            when(questionRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(q));
            when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.deleteQuestion(1L);

            assertThat(q.isDeleted()).isTrue();
            verify(questionRepository).save(q);
        }

        @Test
        @DisplayName("Câu hỏi không tồn tại → QUESTION_NOT_FOUND")
        void delete_notFound_throws() {
            when(questionRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteQuestion(99L))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.QUESTION_NOT_FOUND);
        }
    }

    // =====================================================================
    // Edge: response details
    // =====================================================================

    @Nested
    @DisplayName("submitQuiz - response details")
    class ResponseDetails {

        @Test
        @DisplayName("MULTIPLE_CHOICE detail chứa selectedOptionText + correctOptionText")
        void multipleChoice_detailContainsBoth() {
            QuizQuestion q = question(1L, QuizQuestionType.MULTIPLE_CHOICE, "?");
            QuizOption optA = option(11L, (short) 1, "Hà Nội", false);
            QuizOption optB = option(12L, (short) 2, "Tokyo", true);
            optA.setQuestion(q); optB.setQuestion(q);
            q.getOptions().add(optA); q.getOptions().add(optB);

            QuizSubmitRequest req = QuizSubmitRequest.builder()
                    .answers(List.of(QuizAnswerItem.builder()
                            .questionId(1L).selectedOptionId(11L).build()))
                    .build();

            when(lessonRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(lesson));
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(quizRepository.findByLessonIdAndDeletedFalse(10L)).thenReturn(Optional.of(quiz));
            when(questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(500L))
                    .thenReturn(List.of(q));
            when(optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(1L))
                    .thenReturn(List.of(optA, optB));
            when(progressRepository.findByUserIdAndLessonIdAndDeletedFalse(7L, 10L))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitResponse resp = service.submitQuiz(10L, req);
            QuizDetailItemResponse detail = resp.getDetails().get(0);

            assertThat(detail.getSelectedOptionId()).isEqualTo(11L);
            assertThat(detail.getSelectedOptionText()).isEqualTo("Hà Nội");
            assertThat(detail.getCorrectOptionId()).isEqualTo(12L);
            assertThat(detail.getCorrectOptionText()).isEqualTo("Tokyo");
            assertThat(detail.getQuestionText()).isEqualTo("?");
        }
    }
}
