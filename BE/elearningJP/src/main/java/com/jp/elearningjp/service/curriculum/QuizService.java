package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.ItemBulkReorderRequest;
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
import com.jp.elearningjp.shared.util.OrderIndexResolver;
import com.jp.elearningjp.dto.request.curriculum.QuizQuestionUpdateRequest;
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
    // READ (MANAGE) — Admin/Teacher xem đề kèm đáp án đúng
    // =========================================================

    /**
     * Lấy đề quiz của lesson kèm <b>đầy đủ đáp án đúng</b> — chỉ dành cho
     * Admin/Teacher dùng trong trang quản lý (sửa / xoá / xem cấu trúc).
     *
     * <p>Khác với {@link #getLessonQuiz(Long)} (ẩn hoàn toàn isCorrect để bảo
     * mật cho học viên), endpoint này trả thêm:</p>
     * <ul>
     *   <li>MCQ: {@code correct}, {@code correctOptionId} cho mỗi option.</li>
     *   <li>FILL_IN_THE_BLANK: {@code correctTextAnswer} (chuỗi đáp án đúng).</li>
     *   <li>MATCHING: {@code correctMatchingAnswers} (map slotKey -> correctText)
     *       + {@code matchKey} của từng option.</li>
     *   <li>Explanation của mỗi câu hỏi (để teacher review).</li>
     * </ul>
     *
     * <p><b>Quyền:</b> Admin hoặc Teacher (chỉ trong course do mình tạo).
     * Nếu teacher khác cố truy cập → 403 {@code FORBIDDEN_LESSON_ACCESS}.</p>
     */
    @Transactional(readOnly = true)
    public LessonQuizManageResponse getLessonQuizManage(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        // Check quyền teacher-owner hoặc admin
        courseAccessGuard.requireOwnerOrAdmin(lesson.getCourse(), ErrorCode.FORBIDDEN_LESSON_ACCESS);

        Quiz quiz = quizRepository.findByLessonIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        return buildLessonQuizManageResponse(quiz, lesson);
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

        // Tính orderIndex. Nếu client gửi target nằm trong khoảng hiện có
        // (1..max), resolveOrderIndex sẽ trả về tempIndex = max+1. Caller sẽ
        // save với orderIndex = tempIndex, rồi gọi swapQuestionToTarget() để
        // dời về target qua thuật toán adjacent swap chain (an toàn UNIQUE).
        Integer resolvedIndex = resolveOrderIndex(quiz.getId(), request.getOrderIndex());
        Integer maxOrder = questionRepository.findMaxOrderIndexByQuizId(quiz.getId());
        int max = (maxOrder != null ? maxOrder : 0);
        Integer requestedIdx = request.getOrderIndex();
        int targetIdx = (requestedIdx != null && requestedIdx > 0)
                ? Math.min(requestedIdx, max + 1) : -1;
        boolean needSwap = targetIdx > 0 && targetIdx <= max;  // target nằm trong khoảng hiện có

        if (needSwap) {
            // Kiểm tra trước: target phải trống
            if (questionRepository.existsByQuizIdAndOrderIndexExcludingId(quiz.getId(), targetIdx)) {
                log.warn("[QUIZ] OrderIndex conflict at addQuestionToLesson for quiz id={}, target={}",
                        quiz.getId(), targetIdx);
                throw new AppException(ErrorCode.QUESTION_ORDER_CONFLICT);
            }
        }

        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .questionText(request.getQuestionText())
                .explanation(request.getExplanation())
                .orderIndex(resolvedIndex)
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

        // Swap chain nếu cần — dùng bulk shift an toàn (không gap) thay vì adjacent swap.
        if (needSwap) {
            int shifted = questionRepository.shiftOrderIndexesUpInRangeExcluding(
                    quiz.getId(), targetIdx, max, savedQuestion.getId());
            log.info("[QUIZ] addQuestion: shifted {} question(s) in [{}..{}] up +1 to make room",
                    shifted, targetIdx, max);
            savedQuestion.setOrderIndex(targetIdx);
            questionRepository.save(savedQuestion);
        }

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
    // UPDATE QUESTION (Admin/Teacher)
    // =========================================================

    /**
     * Cập nhật 1 câu hỏi quiz đã tồn tại. Hỗ trợ partial update:
     *
     * <ul>
     *   <li>Các field text/null → chỉ update khi request gửi giá trị (kể cả null).</li>
     *   <li>{@code orderIndex}: nếu khác giá trị hiện tại → resolve lại vị trí
     *       (giống {@code addQuestionToLesson}) để đảm bảo không trùng và
     *       shift các câu khác nếu cần.</li>
     *   <li>{@code options}: nếu gửi (kể cả mảng rỗng) sẽ <b>THAY THẾ</b>
     *       toàn bộ options hiện tại (xoá cũ + tạo mới). Nếu null → giữ nguyên.</li>
     *   <li>{@code vocabId} / {@code characterId}: truyền rõ ràng {@code null}
     *       để xoá liên kết SRS; truyền số để đổi sang vocab/kanji khác.</li>
     * </ul>
     *
     * <p>Quyền: Admin hoặc Teacher (chỉ trong course do mình tạo).</p>
     */
    @Transactional
    public QuizQuestionResponse updateQuestion(Long questionId, QuizQuestionUpdateRequest request) {
        QuizQuestion question = questionRepository.findByIdWithOptions(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        // Check quyền: admin pass, teacher chỉ update được câu hỏi thuộc course do mình tạo
        courseAccessGuard.requireOwnerOrAdmin(
                question.getQuiz().getLesson().getCourse(),
                ErrorCode.FORBIDDEN_LESSON_ACCESS
        );

        Long quizId = question.getQuiz().getId();
        Integer currentOrderIndex = question.getOrderIndex();

        // ----- 1. Update orderIndex (nếu gửi và khác giá trị hiện tại) -----
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0
                && !request.getOrderIndex().equals(question.getOrderIndex())) {
            Integer newOrder = resolveOrderIndex(quizId, request.getOrderIndex());
            // newOrder = tempIndex (max+1) nếu cần swap chain; = target nếu append
            Integer maxOrderForUpdate = questionRepository.findMaxOrderIndexByQuizId(quizId);
            int maxForUpdate = (maxOrderForUpdate != null ? maxOrderForUpdate : 0);
            int requestedTarget = Math.min(request.getOrderIndex(), maxForUpdate + 1);
            boolean needSwapChain = requestedTarget > 0 && requestedTarget <= maxForUpdate
                    && requestedTarget != currentOrderIndex;

            if (needSwapChain) {
                // Đặt question về tempIndex (= max+1) để giải phóng currentOrderIndex.
                // Sau đó shift các câu trong [target..current-1] lên +1 (nếu move lên)
                // hoặc [current+1..target] xuống -1 (nếu move xuống), rồi set question về target.
                // Dùng bulk UPDATE để tránh vi phạm UNIQUE constraint và tránh gap nếu có.
                int currentIdx = currentOrderIndex;
                int targetIdx = requestedTarget;
                question.setOrderIndex(newOrder);
                QuizQuestion savedTemp = questionRepository.save(question);
                questionRepository.flush();
                if (targetIdx < currentIdx) {
                    // Move lên: shift [target..current-1] lên +1 để giải phóng target
                    int shifted = questionRepository.shiftOrderIndexesUpInRangeExcluding(
                            quizId, targetIdx, currentIdx - 1, savedTemp.getId());
                    log.info("[QUIZ] updateQuestion id={}: shifted {} question(s) in [{}..{}] up +1",
                            savedTemp.getId(), shifted, targetIdx, currentIdx - 1);
                } else if (targetIdx > currentIdx) {
                    // Move xuống: shift [current+1..target] xuống -1
                    int shifted = questionRepository.shiftOrderIndexesDownInRangeExcluding(
                            quizId, currentIdx + 1, targetIdx, savedTemp.getId());
                    log.info("[QUIZ] updateQuestion id={}: shifted {} question(s) in [{}..{}] down -1",
                            savedTemp.getId(), shifted, currentIdx + 1, targetIdx);
                }
                question.setOrderIndex(requestedTarget);
            } else {
                // target > max+1 (append) hoặc target = current (no-op) → chỉ set
                if (requestedTarget > 0) {
                    question.setOrderIndex(requestedTarget);
                }
            }
        }

        // ----- 2. Update các field text/enum -----
        if (request.getQuestionText() != null && !request.getQuestionText().isBlank()) {
            question.setQuestionText(request.getQuestionText());
        }
        if (request.getExplanation() != null) {
            // Cho phép truyền "" để xoá giải thích
            question.setExplanation(request.getExplanation().isBlank() ? null : request.getExplanation());
        }
        if (request.getQuestionType() != null) {
            question.setQuestionType(request.getQuestionType());
        }
        if (request.getCorrectTextAnswer() != null) {
            question.setCorrectTextAnswer(
                    request.getCorrectTextAnswer().isBlank() ? null : request.getCorrectTextAnswer());
        }
        if (request.getMatchingData() != null) {
            question.setMatchingData(request.getMatchingData());
        }

        // ----- 3. Update liên kết vocab / kanji -----
        // Truyền rõ ràng -1 (sentinel) để xoá; truyền số > 0 để đổi.
        // Dùng wrapper để phân biệt "không gửi" (null) vs "gửi null" (xoá).
        if (request.getVocabId() != null) {
            if (request.getVocabId() <= 0) {
                question.setVocab(null);
            } else {
                question.setVocab(entityManager.getReference(Vocabulary.class, request.getVocabId()));
            }
        }
        if (request.getCharacterId() != null) {
            if (request.getCharacterId() <= 0) {
                question.setCharacter(null);
            } else {
                question.setCharacter(entityManager.getReference(WritingCharacter.class, request.getCharacterId()));
            }
        }

        // ----- 4. Replace options nếu có gửi -----
        if (request.getOptions() != null) {
            // Xoá options cũ (orphanRemoval = true sẽ xoá DB row khi flush)
            question.getOptions().clear();
            // Tạo options mới vào collection hiện tại (giữ reference PersistentBag)
            for (QuizOptionRequest optReq : request.getOptions()) {
                QuizOption option = QuizOption.builder()
                        .question(question)
                        .optionKey(optReq.getOptionKey())
                        .optionText(optReq.getOptionText())
                        .correct(optReq.isCorrect())
                        .matchKey(optReq.getMatchKey())
                        .build();
                question.getOptions().add(option);
            }
        }

        QuizQuestion saved = questionRepository.save(question);

        log.info("[QUIZ] Updated question id={} (quiz id={}, lesson id={})",
                questionId, quizId, question.getQuiz().getLesson().getId());

        // ----- 5. Build response -----
        List<QuizOptionResponse> optionResponses = saved.getOptions().stream()
                .filter(o -> !o.isDeleted())
                .map(opt -> QuizOptionResponse.builder()
                        .id(opt.getId())
                        .optionKey(opt.getOptionKey())
                        .optionText(opt.getOptionText())
                        .build())
                .toList();

        return QuizQuestionResponse.builder()
                .id(saved.getId())
                .questionText(saved.getQuestionText())
                .orderIndex(saved.getOrderIndex())
                .questionType(saved.getQuestionType())
                .matchingData(saved.getMatchingData())
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
        Long quizId = question.getQuiz().getId();
    Long lessonId = question.getQuiz().getLesson().getId();
    // 1) Soft-delete + flush NGAY: partial unique index
    //    (quiz_id, order_index) WHERE deleted = false chỉ tính row active,
    //    flush để chắc chắn slot cũ đã được giải phóng trước khi đánh số lại.
    question.softDelete();
    questionRepository.save(question);
    questionRepository.flush();
    // 2) Renumber 1..N cho các câu còn active → không để lại gap
    List<QuizQuestion> remaining = questionRepository
            .findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(quizId);
    if (!remaining.isEmpty()) {
        OrderIndexResolver.persistSequential(
                remaining,
                QuizQuestion::getId,
                QuizQuestion::setOrderIndex,
                questionRepository::flush
        );
    }
    log.info("[QUIZ] Soft-deleted question id={} (quiz id={}, lesson id={}), renumbered {} remaining",
            questionId, quizId, lessonId, remaining.size());
    }

    /**
     * Sắp xếp lại thứ tự hàng loạt các câu hỏi trong quiz của 1 lesson (drag-drop UI).
     *
     * <p>Quy tắc: list {@code questionIds} phải chứa <b>đúng tất cả</b> câu hỏi active
     * của quiz hiện tại (không thiếu, không thừa, không trùng).</p>
     *
     * <p>Quyền: Admin hoặc Teacher (chỉ reorder trong course do mình tạo).</p>
     *
     * @param lessonId lesson cần reorder câu hỏi
     * @param request  danh sách question ID theo thứ tự MỚI
     * @return danh sách câu hỏi sau khi reorder
     */
    @Transactional
    public List<QuizQuestionResponse> reorderQuizQuestions(Long lessonId, ItemBulkReorderRequest request) {
        Quiz quiz = quizRepository.findByLessonIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        // Check quyền: admin pass, teacher chỉ reorder câu hỏi trong course do mình tạo
        courseAccessGuard.requireOwnerOrAdmin(
                quiz.getLesson().getCourse(),
                ErrorCode.FORBIDDEN_LESSON_ACCESS
        );

        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_EMPTY);
        }

        List<QuizQuestion> currentQuestions = questionRepository
                .findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(quiz.getId());
        Set<Long> currentIds = new HashSet<>();
        for (QuizQuestion q : currentQuestions) currentIds.add(q.getId());

        Set<Long> requestedIds = new HashSet<>(request.getItemIds());
        if (requestedIds.size() != request.getItemIds().size()) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_INVALID);
        }
        if (!requestedIds.equals(currentIds)) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_MISMATCH);
        }

        // 1) Sắp xếp lại trong bộ nhớ theo list client gửi (orderIndex = 1..N)
        OrderIndexResolver.bulkReorder(
                currentQuestions,
                request.getItemIds(),
                QuizQuestion::getId,
                QuizQuestion::setOrderIndex
        );
        // 2) Ghi an toàn 2 pha (marker âm → 1..N), tránh UNIQUE conflict
        OrderIndexResolver.persistSequential(
                currentQuestions,
                QuizQuestion::getId,
                QuizQuestion::setOrderIndex,
                questionRepository::flush
        );

        log.info("[QUIZ] Bulk reordered {} question(s) in quiz id={} (lesson id={})",
                currentQuestions.size(), quiz.getId(), lessonId);
        return currentQuestions.stream()
                .map(q -> QuizQuestionResponse.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .orderIndex(q.getOrderIndex())
                        .questionType(q.getQuestionType())
                        .matchingData(q.getMatchingData())
                        .build())
                .toList();
    }

    // =========================================================
    // INTERNAL HELPERS
    // =========================================================

    /**
     * Tính orderIndex hợp lệ khi thêm câu hỏi mới vào quiz.
     *
     * <p><b>Quy ước:</b></p>
     * <ul>
     *   <li>Client gửi orderIndex hợp lệ (1..MAX+1): dùng thuật toán
     *       <b>ADJACENT SWAP CHAIN</b> — tạo với orderIndex tạm = MAX+1, rồi
     *       swap từng bước về target (xem chi tiết trong
     *       {@code KanjiSectionItemService.createKanjiItem}).</li>
     *   <li>Client không gửi (null hoặc &lt;= 0): luôn lấy <b>MAX + 1</b>
     *       (giống pattern của các service khác — không fill gap).</li>
     *   <li>orderIndex &gt; MAX+1: clamp về MAX+1 (giống pattern của Lesson).</li>
     * </ul>
     *
     * <p>Trước khi insert, method {@code existsByQuizIdAndOrderIndexExcludingId}
     * nên được gọi ở caller để báo lỗi 409 sớm.</p>
     */
    private Integer resolveOrderIndex(Long quizId, Integer requestedIndex) {
        Integer maxOrder = questionRepository.findMaxOrderIndexByQuizId(quizId);
        int max = (maxOrder != null ? maxOrder : 0);

        // Case 1: client không gửi orderIndex -> luôn lấy MAX + 1
        if (requestedIndex == null || requestedIndex <= 0) {
            return max + 1;
        }

        // Case 2: client gửi orderIndex vượt quá max+1 -> clamp
        int target = Math.min(requestedIndex, max + 1);
        if (target < 1) target = 1;

        // Không cần shift: target > max (sẽ làm tempIndex = max+1 luôn)
        if (target > max) {
            return target;
        }

        // Case 3: target nằm trong khoảng hiện có (1..max)
        // Dùng ADJACENT SWAP CHAIN (giống các service khác):
        //   - Caller sẽ save question với orderIndex = max+1 (tempIndex)
        //   - Sau đó swap chain từng bước từ max+1 về target
        //   - Mỗi swap dùng marker tạm âm nên không gây UNIQUE conflict
        // Trả về tempIndex, caller lưu với giá trị này, rồi gọi
        // swapQuestionToTarget(...) để dời về target.
        return max + 1;
    }

    /**
     * Dời 1 quiz question vừa tạo từ tempIndex (= max+1) về targetIndex bằng
     * thuật toán <b>ADJACENT SWAP CHAIN</b>. Mỗi bước swap đều dùng marker tạm
     * âm để không vi phạm UNIQUE constraint.
     *
     * <p>Ví dụ: hiện có [Q1(1), Q2(2), Q3(3), Q4(4)], vừa tạo mới Q5 tại tempIndex=5,
     * client muốn đặt vào orderIndex=2:</p>
     * <pre>
     *   DB: [Q1(1), Q2(2), Q3(3), Q4(4), Q5(5)]
     *   swap Q5@5 ↔ Q4@4 → [Q1(1), Q2(2), Q3(3), Q5(4), Q4(5)]
     *   swap Q5@4 ↔ Q3@3 → [Q1(1), Q2(2), Q5(3), Q3(4), Q4(5)]
     *   swap Q5@3 ↔ Q2@2 → [Q1(1), Q5(2), Q2(3), Q3(4), Q4(5)]   ✓
     * </pre>
     *
     * <p>Được gọi SAU khi đã save question với orderIndex = tempIndex.</p>
     */
    private void swapQuestionToTarget(Long questionId, int tempIndex, int targetIndex) {
        if (targetIndex == tempIndex) {
            log.info("[QUIZ] No swap needed for question id={} (already at target={})",
                    questionId, targetIndex);
            return;
        }
        Long quizIdOfQuestion = questionRepository.findById(questionId)
                .map(qq -> qq.getQuiz().getId())
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        int step = (targetIndex < tempIndex) ? -1 : 1;
        int current = tempIndex;
        int safetyCounter = 0;
        final int MAX_SWAPS = tempIndex + 2;
        while (current != targetIndex && safetyCounter++ < MAX_SWAPS) {
            int nextIndex = current + step;
            Long neighborId = questionRepository.findIdByQuizIdAndOrderIndex(quizIdOfQuestion, nextIndex);
            if (neighborId == null) {
                log.warn("[QUIZ] Cannot find neighbor at orderIndex={} for swap chain (quizId={}, questionId={})",
                        nextIndex, quizIdOfQuestion, questionId);
                break;
            }
            questionRepository.swapOrderIndexes(questionId, nextIndex, neighborId, current);
            questionRepository.flush();
            current = nextIndex;
        }
        log.info("[QUIZ] Swapped question id={} from tempIndex={} to orderIndex={} (quizId={})",
                questionId, tempIndex, current, quizIdOfQuestion);
    }

    /**
     * Comparator sort matchKey cho MATCHING theo dạng "L{i}=R{j}".
     * <p>Parse số từ "L&lt;n&gt;" và "R&lt;m&gt;", so sánh theo (n, m).
     * Nếu matchKey không khớp format thì fallback alphabetical.</p>
     *
     * <p>Đảm bảo thứ tự JSON trả về là L1=R1, L1=R2, L2=R1, L2=R2, ...
     * → dễ debug, dễ test, không phụ thuộc thứ tự insert vào Map.</p>
     */
    private static final Comparator<String> MATCHING_KEY_ORDER = (a, b) -> {
        int[] pa = parseMatchingKey(a);
        int[] pb = parseMatchingKey(b);
        if (pa != null && pb != null) {
            if (pa[0] != pb[0]) return Integer.compare(pa[0], pb[0]);
            return Integer.compare(pa[1], pb[1]);
        }
        // Fallback alphabetical nếu 1 trong 2 không khớp format
        if (pa == null && pb == null) return a.compareTo(b);
        return pa == null ? -1 : 1;
    };

    /**
     * Parse matchKey dạng "L&lt;n&gt;=R&lt;m&gt;" (hoặc "L&lt;n&gt;+R&lt;m&gt;" cũ)
     * → trả về int[]{n, m}. Trả về null nếu không khớp.
     */
    private static int[] parseMatchingKey(String key) {
        if (key == null) return null;
        // Hỗ trợ cả format mới "L{n}=R{m}" và cũ "L{n}→R{m}" / "L{n}+R{m}"
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^L(\\d+)\\s*[=→+]\\s*R(\\d+)$")
                .matcher(key.trim());
        if (!m.matches()) return null;
        try {
            return new int[]{ Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) };
        } catch (NumberFormatException e) {
            return null;
        }
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
     * Build {@link LessonQuizManageResponse} từ entity + lesson (đã load questions + options).
     *
     * <p>Khác {@code buildLessonQuizResponse} ở chỗ trả <b>đầy đủ đáp án đúng</b>
     * để Admin/Teacher review và edit. Map đáp án cho MATCHING được dựng từ
     * options có {@code matchKey} (giống cách {@code gradeQuiz} dựng).</p>
     */
    private LessonQuizManageResponse buildLessonQuizManageResponse(Quiz quiz, Lesson lesson) {
        List<QuizQuestion> questions = (quiz.getQuestions() == null || quiz.getQuestions().isEmpty())
                ? questionRepository.findByQuizIdAndDeletedFalseOrderByOrderIndexAsc(quiz.getId())
                : new ArrayList<>(quiz.getQuestions());

        List<QuizQuestionWithAnswerResponse> questionResponses = questions.stream()
                .filter(q -> !q.isDeleted())
                .map(q -> {
                    List<QuizOption> options = (q.getOptions() == null || q.getOptions().isEmpty())
                            ? optionRepository.findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(q.getId())
                            : new ArrayList<>(q.getOptions());

                    List<QuizQuestionWithAnswerResponse.QuizOptionWithAnswer> optionResponses =
                            options.stream()
                                    .filter(o -> !o.isDeleted())
                                    .map(opt -> QuizQuestionWithAnswerResponse.QuizOptionWithAnswer.builder()
                                            .id(opt.getId())
                                            .optionKey(opt.getOptionKey())
                                            .optionText(opt.getOptionText())
                                            .correct(opt.isCorrect())
                                            .matchKey(opt.getMatchKey())
                                            .build())
                                    .toList();

                    // Dựng map slotKey -> correctText cho MATCHING.
                    // Dùng TreeMap với comparator sort matchKey theo dạng "L{i}=R{j}"
                    // (vd "L1=R1", "L1=R2", "L2=R1", ...) để JSON trả về có thứ tự
                    // nhất quán — dễ debug, dễ test, không phụ thuộc vào thứ tự
                    // optionKey do FE gửi lên. Nếu matchKey không khớp format
                    // "L<num>=R<num>" thì fallback alphabetical.
                    Map<String, String> correctMatching = new TreeMap<>(MATCHING_KEY_ORDER);
                    if (q.getQuestionType() == QuizQuestionType.MATCHING) {
                        for (QuizOption opt : options) {
                            if (opt.getMatchKey() != null && !opt.getMatchKey().isBlank()) {
                                correctMatching.put(opt.getMatchKey(), opt.getOptionText());
                            }
                        }
                    }

                    return QuizQuestionWithAnswerResponse.builder()
                            .id(q.getId())
                            .questionText(q.getQuestionText())
                            .orderIndex(q.getOrderIndex())
                            .questionType(q.getQuestionType())
                            .explanation(q.getExplanation())
                            .matchingData(q.getMatchingData())
                            .correctTextAnswer(q.getCorrectTextAnswer())
                            .correctMatchingAnswers(correctMatching)
                            .options(optionResponses)
                            .build();
                })
                .toList();

        return LessonQuizManageResponse.builder()
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

                // Dùng TreeMap với comparator sort matchKey theo "L{i}=R{j}"
                // (vd "L1=R1", "L1=R2", "L2=R1", "L2=R2", ...) để JSON trả về
                // có thứ tự nhất quán — dễ debug, dễ test, không phụ thuộc
                // thứ tự optionKey do FE gửi lên. Nếu matchKey không khớp format
                // thì fallback alphabetical.
                Map<String, String> correctPairs = new TreeMap<>(MATCHING_KEY_ORDER);
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

                // Cũng sort userPairs theo cùng thứ tự với correctPairs (L1=R1, L2=R2, ...)
                // để JSON trả về có key ordering nhất quán → dễ debug, dễ test.
                Map<String, String> sortedUserPairs = new LinkedHashMap<>();
                for (String key : correctPairs.keySet()) {
                    if (userPairs.containsKey(key)) {
                        sortedUserPairs.put(key, userPairs.get(key));
                    }
                }
                // Bổ sung các key user nhập thừa (không nằm trong correctPairs — nếu có)
                for (Map.Entry<String, String> e : userPairs.entrySet()) {
                    sortedUserPairs.putIfAbsent(e.getKey(), e.getValue());
                }

                double accuracy = validOptionsCount > 0 ? ((double) matchedCorrectCount / validOptionsCount * 100.0) : 0.0;
                isCorrect = (validOptionsCount > 0 && matchedCorrectCount == validOptionsCount);

                detailBuilder.isCorrect(isCorrect)
                        .accuracyPercent(Math.round(accuracy * 10.0) / 10.0)
                        .responseTimeMs(responseTimeMs)
                        .matchingData(question.getMatchingData())
                        .userMatchingAnswers(sortedUserPairs)
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