package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.QuizAnswerItem;
import com.jp.elearningjp.dto.request.curriculum.QuizSubmitRequest;
import com.jp.elearningjp.dto.response.curriculum.*;
import com.jp.elearningjp.entity.curriculum.*;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.*;
import com.jp.elearningjp.repository.srs.SrsItemRepository;
import com.jp.elearningjp.repository.srs.SrsReviewLogRepository;
import com.jp.elearningjp.service.srs.Sm2Algorithm;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.enums.QuizQuestionType;
import com.jp.elearningjp.shared.enums.SrsCardType;
import com.jp.elearningjp.shared.enums.SrsRating;
import com.jp.elearningjp.shared.enums.SrsStatus;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.dto.request.curriculum.QuizOptionRequest;
import com.jp.elearningjp.dto.request.curriculum.QuizQuestionCreateRequest;
import com.jp.elearningjp.entity.srs.SrsItem;
import com.jp.elearningjp.entity.srs.SrsReviewLog;
import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service quản lý Quiz — gắn trực tiếp với Lesson.
 *
 * <p><b>Refactor V20:</b> Quiz chỉ liên kết trực tiếp với {@link Lesson} qua
 * {@code lesson_id} (UNIQUE). 1 lesson = 1 quiz. Không còn qua Section trung gian.</p>
 *
 * <p><b>Quy ước nghiệp vụ:</b></p>
 * <ul>
 *   <li>Quiz gắn trực tiếp với 1 lesson (lesson_id UNIQUE).</li>
 *   <li>Mỗi lesson chỉ có tối đa 1 quiz.</li>
 *   <li>Frontend gọi:
 *     <ul>
 *       <li>{@code GET /lessons/{id}/quiz}              — lấy đề của lesson</li>
 *       <li>{@code POST /lessons/{id}/quiz/submit}      — nộp bài của lesson</li>
 *       <li>{@code DELETE /quiz/questions/{qid}}        — xoá câu hỏi</li>
 *     </ul>
 *   </li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuizService {

    QuizRepository quizRepository;
    QuizQuestionRepository questionRepository;
    QuizOptionRepository optionRepository;
    LessonRepository lessonRepository;
    UserLessonProgressRepository progressRepository;
    CurrentUserService currentUserService;
    CourseAccessGuard courseAccessGuard;
    SrsItemRepository srsItemRepository;
    SrsReviewLogRepository srsReviewLogRepository;
    EntityManager entityManager;

    // =========================================================
    // READ
    // =========================================================

    /**
     * Lấy đề thi trắc nghiệm của bài học.
     * BẢO MẬT: Bắt buộc đã đăng ký khóa học, và ẨN HOÀN TOÀN isCorrect.
     */
    @Transactional(readOnly = true)
    public LessonQuizResponse getLessonQuiz(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        courseAccessGuard.requireEnrolled(lesson.getCourse().getId());

        Quiz quiz = quizRepository.findByLessonIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        return buildLessonQuizResponse(quiz, lesson);
    }

    /**
     * Lấy quiz theo quizId (tiện cho các thao tác CRUD question).
     */
    @Transactional(readOnly = true)
    public LessonQuizResponse getQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        if (quiz.isDeleted()) {
            throw new AppException(ErrorCode.QUIZ_NOT_FOUND);
        }
        // Load questions + options cho response
        loadQuestionsForRead(quiz);
        return buildLessonQuizResponse(quiz, quiz.getLesson());
    }

    // =========================================================
    // SUBMIT
    // =========================================================

    /**
     * Nộp bài làm trắc nghiệm & Chấm điểm tự động tại Server.
     * Nếu điểm >= passingScorePercent (mặc định 80%) -> set bài học COMPLETED.
     */
    @Transactional
    public QuizSubmitResponse submitQuiz(Long lessonId, QuizSubmitRequest request) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        courseAccessGuard.requireEnrolled(lesson.getCourse().getId());
        User currentUser = currentUserService.getCurrentUser();

        Quiz quiz = quizRepository.findByLessonIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        return gradeQuiz(currentUser, lesson, quiz, request);
    }

    // =========================================================
    // CREATE QUESTION (Admin/Teacher)
    // =========================================================

    /**
     * Admin hoặc Teacher (chỉ trong course do mình tạo) thêm 1 câu hỏi vào quiz
     * của lesson. Nếu lesson chưa có quiz thì tự tạo mới với title mặc định và
     * passingScorePercent = 80%.
     */
    @Transactional
    public QuizQuestionResponse addQuestionToLesson(Long lessonId, QuizQuestionCreateRequest request) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        courseAccessGuard.requireOwnerOrAdmin(lesson.getCourse(), ErrorCode.FORBIDDEN_LESSON_ACCESS);

        Quiz quiz = quizRepository.findByLessonIdAndDeletedFalse(lessonId)
                .orElseGet(() -> {
                    Quiz newQuiz = Quiz.builder()
                            .lesson(lesson)
                            .title("Bài kiểm tra: " + lesson.getTitle())
                            .passingScorePercent(80)
                            .build();
                    return quizRepository.save(newQuiz);
                });

        Integer orderIndex = resolveOrderIndex(quiz.getId(), request.getOrderIndex());
        if (questionRepository.existsByQuizIdAndOrderIndexExcludingId(quiz.getId(), orderIndex)) {
            log.warn("[QUIZ] OrderIndex conflict detected at addQuestionToLesson for quiz id={}, orderIndex={}",
                    quiz.getId(), orderIndex);
            throw new AppException(ErrorCode.QUESTION_ORDER_CONFLICT);
        }

        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .questionText(request.getQuestionText())
                .explanation(request.getExplanation())
                .orderIndex(orderIndex)
                .questionType(request.getQuestionType() != null ? request.getQuestionType() : QuizQuestionType.MULTIPLE_CHOICE)
                .correctTextAnswer(request.getCorrectTextAnswer())
                .matchingData(request.getMatchingData())
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
                        .matchKey(optReq.getMatchKey())
                        .build();
                QuizOption savedOpt = optionRepository.save(option);
                optionResponses.add(QuizOptionResponse.builder()
                        .id(savedOpt.getId())
                        .optionKey(savedOpt.getOptionKey())
                        .optionText(savedOpt.getOptionText())
                        .build());
            }
        }

        log.info("[QUIZ] Admin/Teacher added question id={} to lesson id={}", savedQuestion.getId(), lessonId);

        return QuizQuestionResponse.builder()
                .id(savedQuestion.getId())
                .questionText(savedQuestion.getQuestionText())
                .orderIndex(savedQuestion.getOrderIndex())
                .questionType(savedQuestion.getQuestionType())
                .matchingData(savedQuestion.getMatchingData())
                .options(optionResponses)
                .build();
    }

    // =========================================================
    // DELETE
    // =========================================================

    /**
     * Admin hoặc Teacher (chỉ trong course do mình tạo) xoá mềm 1 câu hỏi.
     * Idempotent — gọi lại với câu đã xoá sẽ ném {@code QUESTION_NOT_FOUND}.
     */
    @Transactional
    public void deleteQuestion(Long questionId) {
        QuizQuestion question = questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        // Check quyền: admin pass, teacher chỉ xoá được câu hỏi thuộc course do mình tạo
        courseAccessGuard.requireOwnerOrAdmin(
                question.getQuiz().getLesson().getCourse(),
                ErrorCode.FORBIDDEN_LESSON_ACCESS
        );
        question.softDelete();
        questionRepository.save(question);
        log.info("[QUIZ] Soft-deleted question id={} (quiz id={}, lesson id={})",
                questionId, question.getQuiz().getId(), question.getQuiz().getLesson().getId());
    }

    // =========================================================
    // INTERNAL HELPERS
    // =========================================================

    /**
     * Tính orderIndex hợp lệ khi thêm câu hỏi mới vào quiz.
     *
     * <p><b>Quy ước:</b></p>
     * <ul>
     *   <li>Client gửi orderIndex hợp lệ (1..MAX+1): dời tất cả câu có orderIndex
     *       &gt;= target lên +1 (dùng marker tạm) để chèn vào giữa.</li>
     *   <li>Client không gửi (null hoặc &lt;= 0): fill gap nhỏ nhất; nếu không có
     *       gap thì lấy MAX + 1. Mục đích: tái sử dụng slot trống do xoá mềm
     *       thay vì luôn tăng dần vô tận.</li>
     *   <li>orderIndex &gt; MAX+1: clamp về MAX+1 (giống pattern của Lesson).</li>
     * </ul>
     *
     * <p>Sau khi resolve, method {@code existsByQuizIdAndOrderIndexExcludingId}
     * nên được gọi ở caller để báo lỗi 409 sớm trước khi insert.</p>
     */
    private Integer resolveOrderIndex(Long quizId, Integer requestedIndex) {
        Integer maxOrder = questionRepository.findMaxOrderIndexByQuizId(quizId);
        int max = (maxOrder != null ? maxOrder : 0);

        // Case 1: client không gửi orderIndex -> fill gap nhỏ nhất
        if (requestedIndex == null || requestedIndex <= 0) {
            Integer firstAvailable = questionRepository.findFirstAvailableOrderIndex(quizId);
            return (firstAvailable != null ? firstAvailable : max + 1);
        }

        // Case 2: client gửi orderIndex vượt quá max+1 -> clamp
        int target = Math.min(requestedIndex, max + 1);
        if (target < 1) target = 1;

        // Nếu target > max+1 đã bị clamp, không cần shift
        if (target > max) {
            return target;
        }

        // Case 3: target nằm trong khoảng hiện có (1..max) -> shift UP các câu >= target
        // Vì chưa có câu nào ở target (giả định caller đã check exists trước),
        // shift an toàn không cần marker tạm.
        int shifted = questionRepository.shiftOrderIndexesUp(quizId, target);
        log.info("[QUIZ] Shifted {} question(s) with orderIndex >= {} in quiz id={}",
                shifted, target, quizId);
        return target;
    }

    /**
     * Build {@link LessonQuizResponse} từ entity + lesson (đã load questions + options).
     */
    private LessonQuizResponse buildLessonQuizResponse(Quiz quiz, Lesson lesson) {
        List<QuizQuestion> questions = (quiz.getQuestions() == null || quiz.getQuestions().isEmpty())
                ? questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(quiz.getId())
                : new ArrayList<>(quiz.getQuestions());

        List<QuizQuestionResponse> questionResponses = questions.stream()
                .filter(q -> !q.isDeleted())
                .map(q -> {
                    List<QuizOption> options = (q.getOptions() == null || q.getOptions().isEmpty())
                            ? optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(q.getId())
                            : new ArrayList<>(q.getOptions());
                    List<QuizOptionResponse> optionResponses = options.stream()
                            .filter(o -> !o.isDeleted())
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
                            .questionType(q.getQuestionType())
                            .matchingData(q.getMatchingData())
                            .options(optionResponses)
                            .build();
                })
                .toList();

        return LessonQuizResponse.builder()
                .quizId(quiz.getId())
                .lessonId(lesson.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .passingScorePercent(quiz.getPassingScorePercent())
                .totalQuestions(questionResponses.size())
                .questions(questionResponses)
                .build();
    }

    /**
     * Force load questions + options vào entity (tránh Lazy trong transaction readOnly).
     *
     * <p><b>Lưu ý quan trọng:</b> PHẢI dùng {@code clear() + addAll(...)} trên collection
     * hiện tại của entity, KHÔNG dùng {@code set...(new ArrayList<>(...))}.</p>
     * <p>Vì {@code Quiz.questions} và {@code QuizQuestion.options} đều có
     * {@code cascade = ALL, orphanRemoval = true}, Hibernate wrap chúng bằng
     * {@code PersistentBag} ngay từ lúc {@code save()}. Nếu ta thay thế reference bằng
     * {@code new ArrayList<>(...)}, Hibernate lúc auto-flush sẽ thấy collection cũ
     * "không còn được reference bởi owning entity" và ném
     * {@code HibernateException: A collection with cascade="all-delete-orphan" was no
     * longer referenced by the owning entity instance}.</p>
     */
    private void loadQuestionsForRead(Quiz quiz) {
        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            List<QuizQuestion> qs = questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(quiz.getId());
            // ✅ Giữ reference PersistentBag managed bằng clear + addAll
            quiz.getQuestions().clear();
            quiz.getQuestions().addAll(qs);
        }
        for (QuizQuestion q : quiz.getQuestions()) {
            if (q.getOptions() == null || q.getOptions().isEmpty()) {
                List<QuizOption> opts = optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(q.getId());
                // ✅ Tương tự cho options
                q.getOptions().clear();
                q.getOptions().addAll(opts);
            }
        }
    }

    /**
     * Chấm điểm 1 quiz (tách ra để dùng chung cho cả luồng theo lesson / theo section).
     */
    private QuizSubmitResponse gradeQuiz(User currentUser, Lesson lesson, Quiz quiz, QuizSubmitRequest request) {
        List<QuizQuestion> questions = questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(quiz.getId());
        int totalQuestions = questions.size();
        if (totalQuestions == 0) {
            throw new AppException(ErrorCode.QUIZ_NOT_FOUND);
        }

        // Tạo map câu trả lời của học viên
        Map<Long, QuizAnswerItem> userAnswers = (request.getAnswers() != null)
                ? request.getAnswers().stream()
                    .collect(Collectors.toMap(QuizAnswerItem::getQuestionId, item -> item, (a, b) -> a))
                : Collections.emptyMap();

        int correctCount = 0;
        List<QuizDetailItemResponse> details = new ArrayList<>();

        for (QuizQuestion question : questions) {
            QuizAnswerItem answerItem = userAnswers.get(question.getId());
            Integer responseTimeMs = answerItem != null ? answerItem.getResponseTimeMs() : null;
            boolean isCorrect = false;

            QuizDetailItemResponse.QuizDetailItemResponseBuilder detailBuilder = QuizDetailItemResponse.builder()
                    .questionId(question.getId())
                    .questionText(question.getQuestionText())
                    .questionType(question.getQuestionType())
                    .explanation(question.getExplanation());

            if (question.getQuestionType() == com.jp.elearningjp.shared.enums.QuizQuestionType.FILL_IN_THE_BLANK) {
                String userText = (answerItem != null && answerItem.getTextAnswer() != null)
                        ? answerItem.getTextAnswer().trim() : "";
                String correctText = question.getCorrectTextAnswer() != null
                        ? question.getCorrectTextAnswer().trim() : "";
                isCorrect = !correctText.isEmpty() && userText.equalsIgnoreCase(correctText);

                detailBuilder.userTextAnswer(userText)
                        .correctTextAnswer(correctText)
                        .isCorrect(isCorrect);

            } else if (question.getQuestionType() == com.jp.elearningjp.shared.enums.QuizQuestionType.MATCHING) {
                List<QuizOption> options = optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(question.getId());
                Map<String, String> userPairs = (answerItem != null && answerItem.getMatchingAnswers() != null)
                        ? answerItem.getMatchingAnswers()
                        : Collections.emptyMap();

                Map<String, String> correctPairs = new HashMap<>();
                int validOptionsCount = 0;
                int matchedCorrectCount = 0;
                for (QuizOption opt : options) {
                    if (opt.getMatchKey() != null && !opt.getMatchKey().isBlank()) {
                        validOptionsCount++;
                        correctPairs.put(opt.getMatchKey(), opt.getOptionText());
                        String userMatchedValue = userPairs.get(opt.getMatchKey());
                        if (userMatchedValue != null && userMatchedValue.trim().equalsIgnoreCase(opt.getOptionText().trim())) {
                            matchedCorrectCount++;
                        }
                    }
                }

                double accuracy = validOptionsCount > 0 ? ((double) matchedCorrectCount / validOptionsCount * 100.0) : 0.0;
                isCorrect = (validOptionsCount > 0 && matchedCorrectCount == validOptionsCount);

                detailBuilder.isCorrect(isCorrect)
                        .accuracyPercent(Math.round(accuracy * 10.0) / 10.0)
                        .responseTimeMs(responseTimeMs)
                        .matchingData(question.getMatchingData())
                        .userMatchingAnswers(userPairs)
                        .correctMatchingAnswers(correctPairs)
                        .correctTextAnswer("Số ô ghép chính xác: " + matchedCorrectCount + "/" + validOptionsCount
                                + " (" + Math.round(accuracy) + "%)");

            } else {
                // MULTIPLE_CHOICE
                List<QuizOption> options = optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(question.getId());
                QuizOption correctOption = options.stream()
                        .filter(QuizOption::isCorrect)
                        .findFirst()
                        .orElse(null);

                Long selectedOptionId = answerItem != null ? answerItem.getSelectedOptionId() : null;
                QuizOption selectedOption = options.stream()
                        .filter(opt -> opt.getId().equals(selectedOptionId))
                        .findFirst()
                        .orElse(null);

                isCorrect = (selectedOption != null && correctOption != null
                        && selectedOption.getId().equals(correctOption.getId()));

                detailBuilder.selectedOptionId(selectedOptionId)
                        .selectedOptionText(selectedOption != null ? selectedOption.getOptionText() : "Chưa chọn")
                        .correctOptionId(correctOption != null ? correctOption.getId() : null)
                        .correctOptionText(correctOption != null ? correctOption.getOptionText() : "")
                        .responseTimeMs(responseTimeMs)
                        .accuracyPercent(isCorrect ? 100.0 : 0.0)
                        .isCorrect(isCorrect);
            }

            if (isCorrect) {
                correctCount++;
            }

            // Sync sang SRS
            Double accuracyForSrs = (question.getQuestionType() == com.jp.elearningjp.shared.enums.QuizQuestionType.MATCHING)
                    ? detailBuilder.build().getAccuracyPercent()
                    : (isCorrect ? 100.0 : 0.0);
            syncQuestionResultToSrs(currentUser, question, isCorrect, accuracyForSrs, responseTimeMs);

            details.add(detailBuilder.build());
        }

        // Tính điểm
        BigDecimal scorePercent = BigDecimal.valueOf((double) correctCount / totalQuestions * 100)
                .setScale(1, RoundingMode.HALF_UP);
        boolean isPassed = scorePercent.compareTo(BigDecimal.valueOf(quiz.getPassingScorePercent())) >= 0;

        // Cập nhật progress
        UserLessonProgress progress = progressRepository
                .findByUserIdAndLessonIdAndDeletedFalse(currentUser.getId(), lesson.getId())
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
                currentUser.getId(), lesson.getId(), scorePercent, isPassed);

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

    // =========================================================
    // SRS INTEGRATION
    // =========================================================

    private void syncQuestionResultToSrs(User user, QuizQuestion question, boolean isCorrect,
                                         Double accuracyPercent, Integer responseTimeMs) {
        if (question.getVocab() == null && question.getCharacter() == null) {
            return;
        }

        SrsRating rating;
        if (question.getQuestionType() == com.jp.elearningjp.shared.enums.QuizQuestionType.MATCHING) {
            rating = determineMatchingRating(accuracyPercent != null ? accuracyPercent : (isCorrect ? 100.0 : 0.0), responseTimeMs);
        } else {
            rating = determineRating(isCorrect, responseTimeMs);
        }

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

        Sm2Algorithm.CalculationResult calc = Sm2Algorithm.calculate(
                prevEf, prevInterval,
                srsItem.getRepetitionCount(), srsItem.getLapseCount(),
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

    private SrsRating determineMatchingRating(double accuracyPercent, Integer responseTimeMs) {
        if (accuracyPercent < 60.0) return SrsRating.AGAIN;
        if (accuracyPercent < 100.0) return SrsRating.HARD;
        if (responseTimeMs != null && responseTimeMs < 10000) return SrsRating.EASY;
        if (responseTimeMs != null && responseTimeMs > 30000) return SrsRating.HARD;
        return SrsRating.GOOD;
    }

    private SrsRating determineRating(boolean isCorrect, Integer responseTimeMs) {
        if (!isCorrect) return SrsRating.AGAIN;
        if (responseTimeMs == null) return SrsRating.GOOD;
        if (responseTimeMs < 3000) return SrsRating.EASY;
        if (responseTimeMs > 15000) return SrsRating.HARD;
        return SrsRating.GOOD;
    }
}
