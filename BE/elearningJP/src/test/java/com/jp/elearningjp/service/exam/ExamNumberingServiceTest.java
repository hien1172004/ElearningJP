package com.jp.elearningjp.service.exam;

import com.jp.elearningjp.dto.response.exam.AdminExamPreviewResponse;
import com.jp.elearningjp.dto.response.exam.ReviewQuestionResponse;
import com.jp.elearningjp.dto.response.exam.ReviewSectionResponse;
import com.jp.elearningjp.entity.exam.*;
import com.jp.elearningjp.mapper.ExamMapper;
import com.jp.elearningjp.repository.content.ReadingPassageRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.exam.*;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.exam.impl.AdminExamServiceImpl;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.SessionType;
import com.jp.elearningjp.shared.enums.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamNumberingServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamSectionRepository examSectionRepository;

    @Mock
    private ExamSectionQuestionRepository examSectionQuestionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private MondaiTypeRepository mondaiTypeRepository;

    @Mock
    private ReadingPassageRepository readingPassageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private StudentAnswerRepository studentAnswerRepository;

    @Mock
    private ExamMapper examMapper;

    @Mock
    private CourseAccessGuard courseAccessGuard;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private com.jp.elearningjp.service.exam.snapshot.ExamSnapshotService examSnapshotService;

    @Mock
    private ExamPublishedVersionRepository examPublishedVersionRepository;

    @Mock
    private com.jp.elearningjp.service.notification.NotificationService notificationService;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private AdminExamServiceImpl adminExamService;

    private MondaiType taskBased;
    private MondaiType keyPoint;
    private MondaiType kanjiReading;
    private MondaiType orthography;

    @BeforeEach
    void setUp() {
        // Kiểm tra quyền quản lý đề: đề độc lập (không gắn khóa) → không cần phân công
        lenient().when(examRepository.findByIdAndDeletedFalse(anyLong()))
                .thenAnswer(inv -> Optional.of(Exam.builder().title("Đề độc lập").build()));
        taskBased = MondaiType.builder().code("TASK_BASED").nameVi("Hiểu nhiệm vụ").skillType(SkillType.LISTENING).build();
        keyPoint = MondaiType.builder().code("KEY_POINT").nameVi("Hiểu điểm mấu chốt").skillType(SkillType.LISTENING).build();

        kanjiReading = MondaiType.builder().code("KANJI_READING").nameVi("Cách đọc Kanji").skillType(SkillType.VOCABULARY).build();
        orthography = MondaiType.builder().code("ORTHOGRAPHY").nameVi("Cách viết Kanji").skillType(SkillType.VOCABULARY).build();
    }

    @Test
    @DisplayName("Phần LISTENING: Mỗi Mondai mới phải tự động bắt đầu lại từ số 1")
    void testListeningSectionQuestionNumberingRestartsPerMondai() {
        Long examId = 1L;
        Exam exam = Exam.builder().title("JLPT N5 Listening Mock").jlptLevel(JlptLevel.N5).build();
        exam.setId(examId);

        ExamSection listeningSection = ExamSection.builder()
                .exam(exam)
                .sessionType(SessionType.LISTENING)
                .sectionTitle("Nghe hiểu")
                .orderIndex(3)
                .build();
        listeningSection.setId(10L);

        exam.addSection(listeningSection);

        // 3 câu Mondai 1 (TASK_BASED)
        Question q1 = createQuestion(101L, taskBased);
        Question q2 = createQuestion(102L, taskBased);
        Question q3 = createQuestion(103L, taskBased);

        // 2 câu Mondai 2 (KEY_POINT)
        Question q4 = createQuestion(104L, keyPoint);
        Question q5 = createQuestion(105L, keyPoint);

        List<ExamSectionQuestion> esqList = List.of(
                createESQ(1L, exam, listeningSection, q1, 99),
                createESQ(2L, exam, listeningSection, q2, 99),
                createESQ(3L, exam, listeningSection, q3, 99),
                createESQ(4L, exam, listeningSection, q4, 99),
                createESQ(5L, exam, listeningSection, q5, 99)
        );

        when(examRepository.findByIdWithSectionsAndScoreGroups(examId)).thenReturn(Optional.of(exam));
        when(examSectionQuestionRepository.findBySectionIdWithQuestionDetails(10L)).thenReturn(esqList);
        when(examMapper.toAdminPreviewResponse(any())).thenReturn(AdminExamPreviewResponse.builder().id(examId).build());
        when(examMapper.toReviewQuestionResponse(any(Question.class))).thenAnswer(inv -> ReviewQuestionResponse.builder().build());

        AdminExamPreviewResponse preview = adminExamService.getExamPreview(examId);

        ReviewSectionResponse secResp = preview.getSections().get(0);
        List<ReviewQuestionResponse> questions = secResp.getQuestions();

        assertEquals(5, questions.size());

        // Mondai 1 (TASK_BASED): Câu 1, 2, 3
        assertEquals(1, questions.get(0).getQuestionNumber(), "Mondai 1 câu 1 phải mang số 1");
        assertEquals(2, questions.get(1).getQuestionNumber(), "Mondai 1 câu 2 phải mang số 2");
        assertEquals(3, questions.get(2).getQuestionNumber(), "Mondai 1 câu 3 phải mang số 3");

        // Mondai 2 (KEY_POINT): Câu 1, 2 (BẮT ĐẦU LẠI TỪ 1!)
        assertEquals(1, questions.get(3).getQuestionNumber(), "Mondai 2 câu 1 phải BẮT ĐẦU LẠI từ số 1");
        assertEquals(2, questions.get(4).getQuestionNumber(), "Mondai 2 câu 2 phải mang số 2");
    }

    @Test
    @DisplayName("Phần VOCABULARY: Giữ nguyên quy tắc đánh số liên tục 1..N xuyên suốt các Mondai")
    void testVocabularySectionQuestionNumberingIsContinuous() {
        Long examId = 2L;
        Exam exam = Exam.builder().title("JLPT N5 Vocab Mock").jlptLevel(JlptLevel.N5).build();
        exam.setId(examId);

        ExamSection vocabSection = ExamSection.builder()
                .exam(exam)
                .sessionType(SessionType.VOCABULARY)
                .sectionTitle("Từ vựng")
                .orderIndex(1)
                .build();
        vocabSection.setId(20L);

        exam.addSection(vocabSection);

        // 2 câu Mondai 1
        Question q1 = createQuestion(201L, kanjiReading);
        Question q2 = createQuestion(202L, kanjiReading);
        // 2 câu Mondai 2
        Question q3 = createQuestion(203L, orthography);
        Question q4 = createQuestion(204L, orthography);

        List<ExamSectionQuestion> esqList = List.of(
                createESQ(11L, exam, vocabSection, q1, 99),
                createESQ(12L, exam, vocabSection, q2, 99),
                createESQ(13L, exam, vocabSection, q3, 99),
                createESQ(14L, exam, vocabSection, q4, 99)
        );

        when(examRepository.findByIdWithSectionsAndScoreGroups(examId)).thenReturn(Optional.of(exam));
        when(examSectionQuestionRepository.findBySectionIdWithQuestionDetails(20L)).thenReturn(esqList);
        when(examMapper.toAdminPreviewResponse(any())).thenReturn(AdminExamPreviewResponse.builder().id(examId).build());
        when(examMapper.toReviewQuestionResponse(any(Question.class))).thenAnswer(inv -> ReviewQuestionResponse.builder().build());

        AdminExamPreviewResponse preview = adminExamService.getExamPreview(examId);

        ReviewSectionResponse secResp = preview.getSections().get(0);
        List<ReviewQuestionResponse> questions = secResp.getQuestions();

        assertEquals(4, questions.size());

        // Từ vựng: Đánh số liên tục 1..4
        assertEquals(1, questions.get(0).getQuestionNumber());
        assertEquals(2, questions.get(1).getQuestionNumber());
        assertEquals(3, questions.get(2).getQuestionNumber());
        assertEquals(4, questions.get(3).getQuestionNumber());
    }

    private Question createQuestion(Long id, MondaiType mondaiType) {
        Question q = new Question();
        q.setId(id);
        q.setMondaiType(mondaiType);
        q.setOptions(new ArrayList<>());
        q.setQuestionText("Nội dung câu hỏi " + id);
        return q;
    }

    private ExamSectionQuestion createESQ(Long id, Exam exam, ExamSection section, Question question, int qNum) {
        ExamSectionQuestion esq = ExamSectionQuestion.builder()
                .exam(exam)
                .section(section)
                .question(question)
                .questionNumber(qNum)
                .build();
        esq.setId(id);
        return esq;
    }
}
