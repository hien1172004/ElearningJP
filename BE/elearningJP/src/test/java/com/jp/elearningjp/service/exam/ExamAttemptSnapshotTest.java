package com.jp.elearningjp.service.exam;

import com.jp.elearningjp.dto.request.exam.SaveAnswerRequest;
import com.jp.elearningjp.dto.request.exam.StartExamRequest;
import com.jp.elearningjp.dto.request.exam.SubmitExamRequest;
import com.jp.elearningjp.dto.response.exam.AttemptGroupScoreResponse;
import com.jp.elearningjp.dto.response.exam.ExamResultResponse;
import com.jp.elearningjp.dto.response.exam.ExamTakingResponse;
import com.jp.elearningjp.dto.response.exam.TakingQuestionResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.exam.AttemptGroupScore;
import com.jp.elearningjp.entity.exam.Exam;
import com.jp.elearningjp.entity.exam.ExamAttempt;
import com.jp.elearningjp.entity.exam.ExamScoreGroup;
import com.jp.elearningjp.entity.exam.Question;
import com.jp.elearningjp.entity.exam.QuestionOption;
import com.jp.elearningjp.entity.exam.StudentAnswer;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.ExamMapper;
import com.jp.elearningjp.repository.exam.AttemptGroupScoreRepository;
import com.jp.elearningjp.repository.exam.ExamAttemptRepository;
import com.jp.elearningjp.repository.exam.ExamRepository;
import com.jp.elearningjp.repository.exam.ExamSectionQuestionRepository;
import com.jp.elearningjp.repository.exam.ExamSectionRepository;
import com.jp.elearningjp.repository.exam.QuestionRepository;
import com.jp.elearningjp.repository.exam.StudentAnswerRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.exam.impl.ExamAttemptServiceImpl;
import com.jp.elearningjp.service.exam.impl.ExamViewBuilder;
import com.jp.elearningjp.service.exam.snapshot.ExamSnapshot;
import com.jp.elearningjp.service.exam.snapshot.ExamSnapshotService;
import com.jp.elearningjp.shared.enums.AttemptStatus;
import com.jp.elearningjp.shared.enums.AttemptType;
import com.jp.elearningjp.shared.enums.ScoreGroupCode;
import com.jp.elearningjp.shared.enums.SessionType;
import com.jp.elearningjp.shared.enums.SkillType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Đề gắn khóa: học viên làm / lưu đáp án / chấm điểm theo snapshot đã duyệt. */
@ExtendWith(MockitoExtension.class)
class ExamAttemptSnapshotTest {

    @Mock ExamRepository examRepository;
    @Mock ExamSectionRepository examSectionRepository;
    @Mock ExamSectionQuestionRepository examSectionQuestionRepository;
    @Mock QuestionRepository questionRepository;
    @Mock ExamAttemptRepository examAttemptRepository;
    @Mock StudentAnswerRepository studentAnswerRepository;
    @Mock AttemptGroupScoreRepository attemptGroupScoreRepository;
    @Mock UserRepository userRepository;
    @Mock ExamMapper examMapper;
    @Mock ExamViewBuilder examViewBuilder;
    @Mock ExamSnapshotService examSnapshotService;
    @Mock EntityManager entityManager;
    @InjectMocks ExamAttemptServiceImpl service;

    User student;
    Exam exam;
    ExamSnapshot snapshot;
    ExamScoreGroup listeningGroup;

    @BeforeEach
    void setUp() {
        student = User.builder().email("s@test.local").build();
        student.setId(7L);
        Course course = Course.builder().title("N5").slug("n5").build();
        course.setId(1L);
        listeningGroup = ExamScoreGroup.builder().id(2L).groupCode(ScoreGroupCode.LISTENING)
                .maxScaledScore(60).minRequiredScore(19).build();
        exam = Exam.builder().title("Đề N5").totalDurationMinutes(999).published(true).course(course)
                .publishedVersionId(900L).build();
        exam.setId(9L);
        exam.getScoreGroups().add(listeningGroup);

        // Bản đã duyệt: câu 100, đáp án đúng là lựa chọn 1000
        snapshot = ExamSnapshot.builder()
                .sections(new ArrayList<>(List.of(ExamSnapshot.Section.builder()
                        .id(1L).sessionType(SessionType.LISTENING).orderIndex(1).durationMinutes(30).scoreGroupId(2L)
                        .questions(new ArrayList<>(List.of(ExamSnapshot.SectionQuestion.builder()
                                .questionId(100L).questionNumber(1).points(BigDecimal.ONE).skillType(SkillType.LISTENING)
                                .taking(TakingQuestionResponse.builder().id(100L).questionNumber(1).build())
                                .optionIds(Set.of(1000L, 1001L)).correctOptionIds(Set.of(1000L))
                                .build())))
                        .build())))
                .build();
        lenient().when(examSnapshotService.find(900L)).thenReturn(Optional.of(snapshot));
    }

    private ExamAttempt snapshotAttempt() {
        ExamAttempt attempt = ExamAttempt.builder().user(student).exam(exam).attemptType(AttemptType.FULL_EXAM)
                .startedAt(Instant.now()).status(AttemptStatus.IN_PROGRESS).publishedVersionId(900L).build();
        attempt.setId(50L);
        when(examAttemptRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(attempt));
        return attempt;
    }

    @Test
    void startExam_usesSnapshot_andPinsVersionOnAttempt() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(student));
        when(examRepository.findByIdWithSectionsAndScoreGroups(9L)).thenReturn(Optional.of(exam));
        when(examSnapshotService.usesSnapshot(exam)).thenReturn(true);
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(examMapper.toTakingResponse(any())).thenReturn(new ExamTakingResponse());

        ExamTakingResponse resp = service.startExam(9L, 7L, StartExamRequest.builder().attemptType(AttemptType.FULL_EXAM).build());

        ArgumentCaptor<ExamAttempt> saved = ArgumentCaptor.forClass(ExamAttempt.class);
        verify(examAttemptRepository).save(saved.capture());
        assertThat(saved.getValue().getPublishedVersionId()).isEqualTo(900L);
        assertThat(resp.getSections()).hasSize(1);
        assertThat(resp.getSections().get(0).getQuestions()).extracting(TakingQuestionResponse::getId).containsExactly(100L);
        verifyNoInteractions(examSectionQuestionRepository);
    }

    @Test
    void saveAnswer_questionOrOptionOutsideSnapshot_rejected() {
        snapshotAttempt();

        assertThatThrownBy(() -> service.saveAnswer(50L, 7L,
                SaveAnswerRequest.builder().questionId(555L).selectedOptionId(1000L).build()))
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXAM_QUESTION_NOT_FOUND);
        assertThatThrownBy(() -> service.saveAnswer(50L, 7L,
                SaveAnswerRequest.builder().questionId(100L).selectedOptionId(7777L).build()))
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXAM_OPTION_NOT_FOUND);
        verify(studentAnswerRepository, never()).save(any());
    }

    @Test
    void submitExam_gradesWithSnapshotAnswerKey_notDraft() {
        ExamAttempt attempt = snapshotAttempt();
        when(examRepository.findByIdWithSectionsAndScoreGroups(9L)).thenReturn(Optional.of(exam));
        // Bản đang soạn đã đổi đáp án: lựa chọn 1000 giờ là sai — học viên vẫn được chấm theo bản đã duyệt
        Question question = new Question();
        question.setId(100L);
        QuestionOption chosen = QuestionOption.builder().question(question).correct(false).build();
        chosen.setId(1000L);
        StudentAnswer answer = StudentAnswer.builder().attempt(attempt).exam(exam).question(question)
                .selectedOption(chosen).build();
        when(studentAnswerRepository.findByAttemptIdWithDetails(50L)).thenReturn(List.of(answer));
        when(attemptGroupScoreRepository.save(any(AttemptGroupScore.class))).thenAnswer(inv -> inv.getArgument(0));
        when(examMapper.toAttemptGroupScoreResponse(any())).thenReturn(new AttemptGroupScoreResponse());
        when(examMapper.toResultResponse(any())).thenReturn(new ExamResultResponse());

        service.submitExam(50L, 7L, new SubmitExamRequest());

        assertThat(answer.isCorrect()).isTrue();
        assertThat(answer.getPointsEarned()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        verifyNoInteractions(examSectionQuestionRepository);
    }
}
