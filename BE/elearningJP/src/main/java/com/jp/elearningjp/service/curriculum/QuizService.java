package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.QuizAnswerItem;
import com.jp.elearningjp.dto.request.curriculum.QuizOptionRequest;
import com.jp.elearningjp.dto.request.curriculum.QuizQuestionCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.QuizSubmitRequest;
import com.jp.elearningjp.dto.response.curriculum.*;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.*;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.*;
import com.jp.elearningjp.repository.srs.SrsItemRepository;
import com.jp.elearningjp.repository.srs.SrsReviewLogRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.srs.Sm2Algorithm;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.enums.SrsCardType;
import com.jp.elearningjp.shared.enums.SrsRating;
import com.jp.elearningjp.shared.enums.SrsStatus;
import com.jp.elearningjp.entity.srs.SrsItem;
import com.jp.elearningjp.entity.srs.SrsReviewLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuizService {

    QuizRepository quizRepository;
    QuizQuestionRepository questionRepository;
    QuizOptionRepository optionRepository;
    LessonRepository lessonRepository;
    CourseEnrollmentRepository enrollmentRepository;
    UserLessonProgressRepository progressRepository;
    UserRepository userRepository;
    SrsItemRepository srsItemRepository;
    SrsReviewLogRepository srsReviewLogRepository;

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Lấy đề thi trắc nghiệm của bài học
     * BẢO MẬT: Bắt buộc đã đăng ký khóa học, và ẨN HOÀN TOÀN isCorrect
     */
    @Transactional(readOnly = true)
    public LessonQuizResponse getLessonQuiz(Long lessonId) {
        User currentUser = getCurrentUserRequired();

        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Long courseId = lesson.getCourse().getId();

        // 1. Kiểm tra quyền truy cập: Bắt buộc phải là ADMIN hoặc ĐÃ ĐĂNG KÝ KHÓA HỌC
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()));

        if (!isAdmin) {
            boolean isEnrolled = enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(currentUser.getId(), courseId);
            if (!isEnrolled) {
                log.warn("[SECURITY] User id={} attempted to access quiz of lesson id={} without enrollment", currentUser.getId(), lessonId);
                throw new AppException(ErrorCode.COURSE_NOT_ENROLLED);
            }
        }

        // 2. Lấy thông tin bài quiz
        Quiz quiz = quizRepository.findByLessonIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        // 3. Lấy danh sách câu hỏi và lựa chọn (chống lộ đáp án đúng)
        List<QuizQuestion> questions = questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(quiz.getId());

        List<QuizQuestionResponse> questionResponses = questions.stream()
                .map(q -> {
                    List<QuizOption> options = optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(q.getId());
                    List<QuizOptionResponse> optionResponses = options.stream()
                            .map(opt -> QuizOptionResponse.builder()
                                    .id(opt.getId())
                                    .optionKey(opt.getOptionKey())
                                    .optionText(opt.getOptionText())
                                    .build())
                            .toList();

                    return QuizQuestionResponse.builder()
                            .id(q.getId())
                            .questionText(q.getQuestionText())
                            .orderIndex(q.getOrderIndex())
                            .options(optionResponses)
                            .build();
                })
                .toList();

        return LessonQuizResponse.builder()
                .quizId(quiz.getId())
                .lessonId(lessonId)
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .passingScorePercent(quiz.getPassingScorePercent())
                .totalQuestions(questionResponses.size())
                .questions(questionResponses)
                .build();
    }

    /**
     * Nộp bài làm trắc nghiệm & Chấm điểm tự động tại Server
     * Nếu điểm >= passingScorePercent (mặc định 80%) -> set bài học COMPLETED
     */
    @Transactional
    public QuizSubmitResponse submitQuiz(Long lessonId, QuizSubmitRequest request) {
        User currentUser = getCurrentUserRequired();

        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Long courseId = lesson.getCourse().getId();

        boolean isEnrolled = enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(currentUser.getId(), courseId);
        if (!isEnrolled) {
            throw new AppException(ErrorCode.COURSE_NOT_ENROLLED);
        }

        Quiz quiz = quizRepository.findByLessonIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        List<QuizQuestion> questions = questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(quiz.getId());
        int totalQuestions = questions.size();
        if (totalQuestions == 0) {
            throw new AppException(ErrorCode.QUIZ_NOT_FOUND);
        }

        // Tạo map câu trả lời của học viên: QuestionId -> QuizAnswerItem
        Map<Long, QuizAnswerItem> userAnswers = (request.getAnswers() != null)
                ? request.getAnswers().stream()
                    .collect(Collectors.toMap(QuizAnswerItem::getQuestionId, item -> item, (a, b) -> a))
                : Collections.emptyMap();

        int correctCount = 0;
        List<QuizDetailItemResponse> details = new ArrayList<>();

        for (QuizQuestion question : questions) {
            List<QuizOption> options = optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(question.getId());
            QuizOption correctOption = options.stream()
                    .filter(QuizOption::isCorrect)
                    .findFirst()
                    .orElse(null);

            QuizAnswerItem answerItem = userAnswers.get(question.getId());
            Long selectedOptionId = answerItem != null ? answerItem.getSelectedOptionId() : null;
            Integer responseTimeMs = answerItem != null ? answerItem.getResponseTimeMs() : null;

            QuizOption selectedOption = options.stream()
                    .filter(opt -> opt.getId().equals(selectedOptionId))
                    .findFirst()
                    .orElse(null);

            boolean isCorrect = (selectedOption != null && correctOption != null && selectedOption.getId().equals(correctOption.getId()));
            if (isCorrect) {
                correctCount++;
            }

            // Tự động đồng bộ kết quả câu hỏi trắc nghiệm vào SRS
            syncQuestionResultToSrs(currentUser, question, isCorrect, responseTimeMs);

            details.add(QuizDetailItemResponse.builder()
                    .questionId(question.getId())
                    .questionText(question.getQuestionText())
                    .selectedOptionId(selectedOptionId)
                    .selectedOptionText(selectedOption != null ? selectedOption.getOptionText() : "Chưa chọn")
                    .correctOptionId(correctOption != null ? correctOption.getId() : null)
                    .correctOptionText(correctOption != null ? correctOption.getOptionText() : "")
                    .isCorrect(isCorrect)
                    .explanation(question.getExplanation())
                    .build());
        }

        // Tính điểm phần trăm
        BigDecimal scorePercent = BigDecimal.valueOf((double) correctCount / totalQuestions * 100)
                .setScale(1, RoundingMode.HALF_UP);

        boolean isPassed = scorePercent.compareTo(BigDecimal.valueOf(quiz.getPassingScorePercent())) >= 0;

        // Cập nhật tiến độ học tập của người dùng
        UserLessonProgress progress = progressRepository
                .findByUserIdAndLessonIdAndDeletedFalse(currentUser.getId(), lessonId)
                .orElseGet(() -> UserLessonProgress.builder()
                        .user(currentUser)
                        .lesson(lesson)
                        .startedAt(Instant.now())
                        .build());

        BigDecimal currentBest = progress.getBestScore() != null ? progress.getBestScore() : BigDecimal.ZERO;
        progress.setBestScore(scorePercent.max(currentBest));
        progress.setLastAccessedAt(Instant.now());

        if (isPassed) {
            progress.setStatus(ProgressStatus.COMPLETED);
            if (progress.getCompletedAt() == null) {
                progress.setCompletedAt(Instant.now());
            }
        }
        progressRepository.save(progress);

        log.info("[QUIZ] User id={} submitted quiz for lesson id={}, score={} %, isPassed={}",
                currentUser.getId(), lessonId, scorePercent, isPassed);

        return QuizSubmitResponse.builder()
                .totalQuestions(totalQuestions)
                .correctCount(correctCount)
                .scorePercent(scorePercent)
                .passingScorePercent(quiz.getPassingScorePercent())
                .isPassed(isPassed)
                .lessonStatus(progress.getStatus())
                .bestScore(progress.getBestScore())
                .details(details)
                .build();
    }

    /**
     * Admin thêm câu hỏi trắc nghiệm vào bài học (tự động tạo Quiz nếu bài học chưa có)
     */
    @Transactional
    public QuizQuestionResponse addQuestionToLesson(Long lessonId, QuizQuestionCreateRequest request) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Quiz quiz = quizRepository.findByLessonIdAndDeletedFalse(lessonId)
                .orElseGet(() -> {
                    Quiz newQuiz = Quiz.builder()
                            .lesson(lesson)
                            .title("Bài kiểm tra: " + lesson.getTitle())
                            .passingScorePercent(80)
                            .build();
                    return quizRepository.save(newQuiz);
                });

        Integer orderIndex = request.getOrderIndex();
        if (orderIndex == null || orderIndex <= 0) {
            Integer maxOrder = questionRepository.findMaxOrderIndexByQuizId(quiz.getId());
            orderIndex = (maxOrder != null ? maxOrder : 0) + 1;
        }

        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .questionText(request.getQuestionText())
                .explanation(request.getExplanation())
                .orderIndex(orderIndex)
                .build();

        if (request.getVocabId() != null) {
            question.setVocab(entityManager.getReference(Vocabulary.class, request.getVocabId()));
        }
        if (request.getCharacterId() != null) {
            question.setCharacter(entityManager.getReference(WritingCharacter.class, request.getCharacterId()));
        }

        QuizQuestion savedQuestion = questionRepository.save(question);

        List<QuizOptionResponse> optionResponses = new ArrayList<>();
        if (request.getOptions() != null) {
            for (QuizOptionRequest optReq : request.getOptions()) {
                QuizOption option = QuizOption.builder()
                        .question(savedQuestion)
                        .optionKey(optReq.getOptionKey())
                        .optionText(optReq.getOptionText())
                        .correct(optReq.isCorrect())
                        .build();
                QuizOption savedOpt = optionRepository.save(option);
                optionResponses.add(QuizOptionResponse.builder()
                        .id(savedOpt.getId())
                        .optionKey(savedOpt.getOptionKey())
                        .optionText(savedOpt.getOptionText())
                        .build());
            }
        }

        log.info("[QUIZ] Admin added question id={} to lesson id={}", savedQuestion.getId(), lessonId);

        return QuizQuestionResponse.builder()
                .id(savedQuestion.getId())
                .questionText(savedQuestion.getQuestionText())
                .orderIndex(savedQuestion.getOrderIndex())
                .options(optionResponses)
                .build();
    }

    /**
     * Admin xóa câu hỏi trắc nghiệm
     */
    @Transactional
    public void deleteQuestion(Long questionId) {
        QuizQuestion question = questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        question.softDelete();
        questionRepository.save(question);
        log.info("[QUIZ] Admin soft-deleted question id={}", questionId);
    }

    // =========================================================
    // SRS INTEGRATION HELPER METHODS
    // =========================================================

    private void syncQuestionResultToSrs(User user, QuizQuestion question, boolean isCorrect, Integer responseTimeMs) {
        // Chỉ xử lý nếu câu hỏi gắn liền với từ vựng hoặc chữ Hán
        if (question.getVocab() == null && question.getCharacter() == null) {
            return;
        }

        SrsRating rating = determineRating(isCorrect, responseTimeMs);

        SrsItem srsItem = null;
        if (question.getVocab() != null) {
            srsItem = srsItemRepository.findByUserIdAndVocabIdAndDeletedFalse(user.getId(), question.getVocab().getId())
                    .orElseGet(() -> SrsItem.builder()
                            .user(user)
                            .vocab(question.getVocab())
                            .cardType(SrsCardType.STANDARD)
                            .easeFactor(new BigDecimal("2.50"))
                            .intervalDays(0)
                            .repetitionCount(0)
                            .lapseCount(0)
                            .status(SrsStatus.LEARNING)
                            .nextReviewAt(Instant.now())
                            .build());
        } else if (question.getCharacter() != null) {
            srsItem = srsItemRepository.findByUserIdAndCharacterIdAndDeletedFalse(user.getId(), question.getCharacter().getId())
                    .orElseGet(() -> SrsItem.builder()
                            .user(user)
                            .character(question.getCharacter())
                            .cardType(SrsCardType.STANDARD)
                            .easeFactor(new BigDecimal("2.50"))
                            .intervalDays(0)
                            .repetitionCount(0)
                            .lapseCount(0)
                            .status(SrsStatus.LEARNING)
                            .nextReviewAt(Instant.now())
                            .build());
        }

        if (srsItem == null) return;

        BigDecimal prevEf = srsItem.getEaseFactor();
        Integer prevInterval = srsItem.getIntervalDays();

        // Chạy thuật toán SM-2
        Sm2Algorithm.CalculationResult calc = Sm2Algorithm.calculate(
                prevEf,
                prevInterval,
                srsItem.getRepetitionCount(),
                srsItem.getLapseCount(),
                rating
        );

        srsItem.setEaseFactor(calc.getNewEf());
        srsItem.setIntervalDays(calc.getNewInterval());
        srsItem.setRepetitionCount(calc.getNewRepetitionCount());
        srsItem.setLapseCount(calc.getNewLapseCount());
        srsItem.setStatus(calc.getNewStatus());
        srsItem.setNextReviewAt(calc.getNextReviewAt());
        srsItem.setLastReviewedAt(Instant.now());
        srsItemRepository.save(srsItem);

        // Lưu vết lịch sử ôn tập vào srs_review_logs
        SrsReviewLog logEntity = SrsReviewLog.builder()
                .srsItem(srsItem)
                .user(user)
                .rating(rating.name())
                .scoreQ(rating.getQualityScore())
                .previousInterval(prevInterval)
                .newInterval(calc.getNewInterval())
                .previousEf(prevEf)
                .newEf(calc.getNewEf())
                .newStatus(calc.getNewStatus())
                .responseTimeMs(responseTimeMs)
                .reviewedAt(Instant.now())
                .build();
        srsReviewLogRepository.save(logEntity);
    }

    private SrsRating determineRating(boolean isCorrect, Integer responseTimeMs) {
        if (!isCorrect) {
            return SrsRating.AGAIN; // Sai -> Học lại ngay
        }
        if (responseTimeMs == null) {
            return SrsRating.GOOD;  // Mặc định chuẩn nếu không đo thời gian
        }
        if (responseTimeMs < 3000) {
            return SrsRating.EASY;  // Dưới 3s -> Quá nhanh, quá dễ
        } else if (responseTimeMs > 15000) {
            return SrsRating.HARD;  // Trên 15s -> Khó khăn, đắn đo
        } else {
            return SrsRating.GOOD;  // 3s - 15s -> Phản xạ tốt
        }
    }

    public User getCurrentUserRequired() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userRepository.findUserByEmail(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
