package com.jp.elearningjp.service.exam.impl;

import com.jp.elearningjp.dto.request.exam.SaveAnswerRequest;
import com.jp.elearningjp.dto.request.exam.StartExamRequest;
import com.jp.elearningjp.dto.request.exam.SubmitExamRequest;
import com.jp.elearningjp.dto.response.exam.*;
import com.jp.elearningjp.entity.exam.*;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.ExamMapper;
import com.jp.elearningjp.repository.exam.*;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.exam.ExamAttemptService;
import com.jp.elearningjp.shared.enums.AttemptStatus;
import com.jp.elearningjp.shared.enums.AttemptType;
import com.jp.elearningjp.shared.enums.ScoreGroupCode;
import com.jp.elearningjp.shared.enums.SkillType;
import com.jp.elearningjp.service.exam.snapshot.ExamSnapshot;
import com.jp.elearningjp.service.exam.snapshot.ExamSnapshotService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExamAttemptServiceImpl implements ExamAttemptService {

    private final ExamRepository examRepository;
    private final ExamSectionRepository examSectionRepository;
    private final ExamSectionQuestionRepository examSectionQuestionRepository;
    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final AttemptGroupScoreRepository attemptGroupScoreRepository;
    private final UserRepository userRepository;
    private final ExamMapper examMapper;
    private final ExamViewBuilder examViewBuilder;
    private final ExamSnapshotService examSnapshotService;
    private final EntityManager entityManager;

    @Override
    public ExamTakingResponse startExam(Long examId, Long userId, StartExamRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Exam exam = examRepository.findByIdWithSectionsAndScoreGroups(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        if (!exam.isPublished()) {
            throw new AppException(ErrorCode.EXAM_NOT_PUBLISHED);
        }

        // Học viên làm đề gắn khóa theo bản đã duyệt
        if (examSnapshotService.usesSnapshot(exam)) {
            return startFromSnapshot(user, exam, request);
        }

        ExamSection targetSection = null;
        int targetDurationMinutes = exam.getTotalDurationMinutes();

        if (request.getAttemptType() == AttemptType.PRACTICE_SECTION) {
            if (request.getSectionId() == null) {
                throw new AppException(ErrorCode.EXAM_SECTION_NOT_FOUND);
            }
            targetSection = examSectionRepository.findByIdAndDeletedFalse(request.getSectionId())
                    .orElseThrow(() -> new AppException(ErrorCode.EXAM_SECTION_NOT_FOUND));

            if (!targetSection.getExam().getId().equals(examId)) {
                throw new AppException(ErrorCode.EXAM_SECTION_NOT_FOUND);
            }
            targetDurationMinutes = targetSection.getDurationMinutes() != null
                    ? targetSection.getDurationMinutes() : 30;
        }

        ExamAttempt attempt = ExamAttempt.builder()
                .user(user)
                .exam(exam)
                .attemptType(request.getAttemptType())
                .section(targetSection)
                .targetDurationMinutes(targetDurationMinutes)
                .startedAt(Instant.now())
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        attempt = examAttemptRepository.save(attempt);

        List<ExamSection> sectionsToLoad;
        if (targetSection != null) {
            sectionsToLoad = List.of(targetSection);
        } else {
            sectionsToLoad = exam.getSections().stream()
                    .filter(s -> !s.isDeleted())
                    .sorted(Comparator.comparingInt(ExamSection::getOrderIndex))
                    .collect(Collectors.toList());
        }

        List<TakingSectionResponse> sectionResponses = sectionsToLoad.stream()
                .map(s -> {
                    var sectionQuestions = examSectionQuestionRepository
                            .findBySectionIdWithQuestionDetails(s.getId());

                    var questionResponses = sectionQuestions.stream()
                            .map(sq -> examViewBuilder.toTakingQuestionResponse(sq.getQuestion(), sq.getQuestionNumber()))
                            .collect(Collectors.toList());

                    return TakingSectionResponse.builder()
                            .id(s.getId())
                            .sessionType(s.getSessionType())
                            .sectionTitle(s.getSectionTitle())
                            .audioUrl(s.getAudioUrl())
                            .orderIndex(s.getOrderIndex())
                            .durationMinutes(s.getDurationMinutes())
                            .questions(questionResponses)
                            .build();
                })
                .collect(Collectors.toList());

        ExamTakingResponse takingResp = examMapper.toTakingResponse(attempt);
        takingResp.setSections(sectionResponses);
        return takingResp;
    }

    @Override
    public void saveAnswer(Long attemptId, Long userId, SaveAnswerRequest request) {
        ExamAttempt attempt = getAuthorizedAttempt(attemptId, userId);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.EXAM_ATTEMPT_ALREADY_SUBMITTED);
        }

        if (attempt.getPublishedVersionId() != null) {
            saveSnapshotAnswer(attempt, attemptQuestions(attempt, requireSnapshot(attempt)), request);
            return;
        }

        Question question = questionRepository.findByIdWithOptionsAndPassage(request.getQuestionId())
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_QUESTION_NOT_FOUND));

        QuestionOption selectedOption = null;
        if (request.getSelectedOptionId() != null) {
            selectedOption = question.getOptions().stream()
                    .filter(o -> o.getId().equals(request.getSelectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.EXAM_OPTION_NOT_FOUND));
        }

        upsertAnswer(attempt, question, selectedOption, request);
    }

    /** Lượt thi theo snapshot: câu hỏi / lựa chọn phải thuộc đề học viên đang làm. */
    private void saveSnapshotAnswer(ExamAttempt attempt, List<ExamSnapshot.SectionQuestion> questions,
                                    SaveAnswerRequest request) {
        ExamSnapshot.SectionQuestion sq = questions.stream()
                .filter(q -> q.getQuestionId().equals(request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_QUESTION_NOT_FOUND));

        QuestionOption selectedOption = null;
        if (request.getSelectedOptionId() != null) {
            if (!sq.getOptionIds().contains(request.getSelectedOptionId())) {
                throw new AppException(ErrorCode.EXAM_OPTION_NOT_FOUND);
            }
            // Lựa chọn / câu hỏi chỉ xóa mềm nên id trong snapshot luôn còn trong DB
            selectedOption = entityManager.getReference(QuestionOption.class, request.getSelectedOptionId());
        }
        upsertAnswer(attempt, questionRepository.getReferenceById(sq.getQuestionId()), selectedOption, request);
    }

    private void upsertAnswer(ExamAttempt attempt, Question question, QuestionOption selectedOption,
                              SaveAnswerRequest request) {
        StudentAnswer answer = studentAnswerRepository
                .findByAttemptIdAndQuestionIdAndDeletedFalse(attempt.getId(), request.getQuestionId())
                .orElseGet(() -> StudentAnswer.builder()
                        .attempt(attempt)
                        .exam(attempt.getExam())
                        .question(question)
                        .build());

        answer.setSelectedOption(selectedOption);
        if (request.getOrderedOptionIds() != null) {
            answer.setOrderedOptionIds(request.getOrderedOptionIds());
        }
        if (request.getAudioPlayCount() != null) {
            answer.setAudioPlayCount(request.getAudioPlayCount());
        }

        studentAnswerRepository.save(answer);
    }

    @Override
    public ExamResultResponse submitExam(Long attemptId, Long userId, SubmitExamRequest request) {
        ExamAttempt attempt = getAuthorizedAttempt(attemptId, userId);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.EXAM_ATTEMPT_ALREADY_SUBMITTED);
        }

        // Lượt thi theo snapshot: đọc đề 1 lần cho cả lưu đáp án và chấm điểm
        ExamSnapshot snapshot = attempt.getPublishedVersionId() != null ? requireSnapshot(attempt) : null;
        List<ExamSnapshot.SectionQuestion> snapshotQuestions = snapshot != null ? attemptQuestions(attempt, snapshot) : null;

        if (request != null && request.getAnswers() != null) {
            for (SaveAnswerRequest ansReq : request.getAnswers()) {
                if (snapshot != null) {
                    saveSnapshotAnswer(attempt, snapshotQuestions, ansReq);
                } else {
                    saveAnswer(attemptId, userId, ansReq);
                }
            }
        }

        Exam exam = examRepository.findByIdWithSectionsAndScoreGroups(attempt.getExam().getId())
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        int timeSpentSeconds = (int) Duration.between(attempt.getStartedAt(), Instant.now()).getSeconds();

        List<GradeItem> gradeItems = snapshot != null
                ? snapshotGradeItems(attempt, exam, snapshot)
                : liveGradeItems(attempt, exam);

        ExamScoreGroup knowledgeGroup = exam.getScoreGroups().stream()
                .filter(g -> ScoreGroupCode.LANGUAGE_KNOWLEDGE.equals(g.getGroupCode()))
                .findFirst().orElse(null);
        ExamScoreGroup readingGroup = exam.getScoreGroups().stream()
                .filter(g -> ScoreGroupCode.READING.equals(g.getGroupCode()))
                .findFirst().orElse(null);
        ExamScoreGroup listeningGroup = exam.getScoreGroups().stream()
                .filter(g -> ScoreGroupCode.LISTENING.equals(g.getGroupCode()))
                .findFirst().orElse(null);
        ExamScoreGroup combinedKnowledgeReadingGroup = exam.getScoreGroups().stream()
                .filter(g -> ScoreGroupCode.LANGUAGE_KNOWLEDGE_READING.equals(g.getGroupCode()))
                .findFirst().orElse(null);

        Map<Long, ExamScoreGroup> questionToScoreGroupMap = new HashMap<>();
        for (GradeItem item : gradeItems) {
            SkillType skill = item.skill();
            ExamScoreGroup sg = null;

            if (skill == SkillType.LISTENING) {
                sg = listeningGroup != null ? listeningGroup : item.sectionGroup();
            } else if (skill == SkillType.READING) {
                sg = readingGroup != null ? readingGroup : (combinedKnowledgeReadingGroup != null ? combinedKnowledgeReadingGroup : item.sectionGroup());
            } else if (skill == SkillType.VOCABULARY || skill == SkillType.GRAMMAR) {
                sg = knowledgeGroup != null ? knowledgeGroup : (combinedKnowledgeReadingGroup != null ? combinedKnowledgeReadingGroup : item.sectionGroup());
            } else {
                sg = item.sectionGroup();
            }

            if (sg != null) {
                questionToScoreGroupMap.put(item.questionId(), sg);
            }
        }

        List<StudentAnswer> studentAnswers = studentAnswerRepository.findByAttemptIdWithDetails(attemptId);
        Map<Long, StudentAnswer> answerMap = studentAnswers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Function.identity(), (e1, e2) -> e1));

        BigDecimal totalRawScore = BigDecimal.ZERO;
        Map<Long, Integer> groupRawCorrectMap = new HashMap<>();
        Map<Long, Integer> groupRawTotalMap = new HashMap<>();

        for (GradeItem item : gradeItems) {
            Long qId = item.questionId();
            ExamScoreGroup sg = questionToScoreGroupMap.get(qId);
            Long sgId = sg != null ? sg.getId() : -1L;

            groupRawTotalMap.put(sgId, groupRawTotalMap.getOrDefault(sgId, 0) + 1);

            StudentAnswer ans = answerMap.get(qId);
            if (ans != null && ans.getSelectedOption() != null) {
                boolean isCorrect = item.correctOptionIds() == null
                        ? ans.getSelectedOption().isCorrect()
                        : item.correctOptionIds().contains(ans.getSelectedOption().getId());
                ans.setCorrect(isCorrect);
                BigDecimal points = isCorrect ? item.points() : BigDecimal.ZERO;
                ans.setPointsEarned(points);
                totalRawScore = totalRawScore.add(points);

                if (isCorrect) {
                    groupRawCorrectMap.put(sgId, groupRawCorrectMap.getOrDefault(sgId, 0) + 1);
                }
            } else if (ans != null) {
                ans.setCorrect(false);
                ans.setPointsEarned(BigDecimal.ZERO);
            }
        }
        studentAnswerRepository.saveAll(studentAnswers);

        List<AttemptGroupScoreResponse> groupScoreResponses = new ArrayList<>();
        int totalScaledScore = 0;
        boolean hasThresholdFailed = false;

        boolean isPracticeSection = attempt.getAttemptType() == AttemptType.PRACTICE_SECTION;
        ExamSection practiceSection = isPracticeSection ? attempt.getSection() : null;

        Collection<ExamScoreGroup> scoreGroupsToCalculate = exam.getScoreGroups();
        if (isPracticeSection && practiceSection != null) {
            // Lấy tất cả nhóm điểm thực tế xuất hiện trong ca thi này
            Set<ExamScoreGroup> groupsInSection = new LinkedHashSet<>();
            for (GradeItem item : gradeItems) {
                ExamScoreGroup g = questionToScoreGroupMap.get(item.questionId());
                if (g != null) {
                    groupsInSection.add(g);
                }
            }
            if (!groupsInSection.isEmpty()) {
                scoreGroupsToCalculate = groupsInSection;
            } else if (practiceSection.getScoreGroup() != null) {
                scoreGroupsToCalculate = List.of(practiceSection.getScoreGroup());
            }
        }

        int targetMaxScaledScore = 0;
        int targetPassingScore = 0;

        for (ExamScoreGroup sg : scoreGroupsToCalculate) {
            int rawCorrect = groupRawCorrectMap.getOrDefault(sg.getId(), 0);
            int rawTotal = groupRawTotalMap.getOrDefault(sg.getId(), 0);

            int groupMaxScore = sg.getMaxScaledScore();
            int groupMinRequired = sg.getMinRequiredScore();
            boolean checkThreshold = true;

            // Xử lý chuẩn cho ca thi luyện tập lẻ (PRACTICE_SECTION):
            if (isPracticeSection && practiceSection != null) {
                if (ScoreGroupCode.LANGUAGE_KNOWLEDGE_READING.equals(sg.getGroupCode())) {
                    // Với N4 / N5 khi luyện riêng Ca 1 hoặc Ca 2:
                    // Thang điểm của ca này là 60 điểm (50% của 120 điểm tổng).
                    // Điểm liệt 38/120 của N4/N5 chỉ tính khi thi toàn bộ (gộp cả 2 ca).
                    // Vì vậy khi luyện ca lẻ, không áp điểm liệt riêng để tránh đánh trượt oan!
                    groupMaxScore = 60;
                    groupMinRequired = 0;
                    checkThreshold = false;
                } else if (ScoreGroupCode.LISTENING.equals(sg.getGroupCode())) {
                    groupMaxScore = 60;
                    groupMinRequired = 19;
                } else {
                    // N1/N2/N3: LANGUAGE_KNOWLEDGE hoặc READING
                    groupMaxScore = 60;
                    groupMinRequired = 19;
                }
            }

            int scaledScore = 0;
            if (rawTotal > 0) {
                scaledScore = (int) Math.round(((double) rawCorrect / rawTotal) * groupMaxScore);
            }

            boolean isGroupPassed = checkThreshold ? (scaledScore >= groupMinRequired) : (scaledScore >= (int) Math.round(groupMaxScore * 0.5));
            if (checkThreshold && !isGroupPassed) {
                hasThresholdFailed = true;
            }

            totalScaledScore += scaledScore;
            targetMaxScaledScore += groupMaxScore;
            targetPassingScore += (int) Math.round(groupMaxScore * 0.5); // Điểm đạt yêu cầu cho ca luyện tập là 50%

            AttemptGroupScore ags = AttemptGroupScore.builder()
                    .attempt(attempt)
                    .exam(exam)
                    .scoreGroup(sg)
                    .rawCorrectCount(rawCorrect)
                    .rawTotalCount(rawTotal)
                    .scaledScore(scaledScore)
                    .groupPassed(isGroupPassed)
                    .build();
            attemptGroupScoreRepository.save(ags);

            AttemptGroupScoreResponse agsResp = examMapper.toAttemptGroupScoreResponse(ags);
            agsResp.setMaxScaledScore(groupMaxScore);
            agsResp.setMinRequiredScore(groupMinRequired);
            groupScoreResponses.add(agsResp);
        }

        int passingThreshold;
        boolean finalPassed;
        if (isPracticeSection) {
            passingThreshold = targetPassingScore;
            boolean isOverallPassed = totalScaledScore >= passingThreshold;
            finalPassed = isOverallPassed && !hasThresholdFailed;
        } else {
            passingThreshold = exam.getOverallPassingScore() != null ? exam.getOverallPassingScore() : 90;
            targetMaxScaledScore = exam.getTotalScaledMaxScore() != null ? exam.getTotalScaledMaxScore() : 180;
            boolean isOverallPassed = totalScaledScore >= passingThreshold;
            finalPassed = isOverallPassed && !hasThresholdFailed;
        }

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(Instant.now());
        attempt.setTimeSpentSeconds(timeSpentSeconds);
        attempt.setTotalRawScore(totalRawScore);
        attempt.setTotalScaledScore(totalScaledScore);
        attempt.setPassed(finalPassed);
        examAttemptRepository.save(attempt);

        ExamResultResponse result = examMapper.toResultResponse(attempt);
        result.setTotalScaledMaxScore(targetMaxScaledScore);
        result.setOverallPassingScore(passingThreshold);
        result.setHasThresholdFailed(hasThresholdFailed);
        result.setGroupScores(groupScoreResponses);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResultResponse getResult(Long attemptId, Long userId) {
        ExamAttempt attempt = getAuthorizedAttempt(attemptId, userId);

        var groupScores = attemptGroupScoreRepository.findByAttemptIdWithScoreGroup(attemptId).stream()
                .map(examMapper::toAttemptGroupScoreResponse)
                .collect(Collectors.toList());

        boolean hasThresholdFailed = groupScores.stream().anyMatch(g -> !g.isGroupPassed());

        ExamResultResponse result = examMapper.toResultResponse(attempt);
        if (attempt.getAttemptType() == AttemptType.PRACTICE_SECTION) {
            int targetMax = 0;
            int targetPass = 0;
            for (var gs : groupScores) {
                if (ScoreGroupCode.LANGUAGE_KNOWLEDGE_READING.equals(gs.getGroupCode())) {
                    gs.setMaxScaledScore(60);
                    gs.setMinRequiredScore(0);
                } else {
                    gs.setMaxScaledScore(60);
                }
                targetMax += gs.getMaxScaledScore();
                targetPass += (int) Math.round(gs.getMaxScaledScore() * 0.5);
            }
            result.setTotalScaledMaxScore(targetMax > 0 ? targetMax : 60);
            result.setOverallPassingScore(targetPass > 0 ? targetPass : 30);
        }
        result.setHasThresholdFailed(hasThresholdFailed);
        result.setGroupScores(groupScores);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamReviewResponse getReview(Long attemptId, Long userId) {
        ExamAttempt attempt = getAuthorizedAttempt(attemptId, userId);
        Exam exam = attempt.getExam();

        var groupScores = attemptGroupScoreRepository.findByAttemptIdWithScoreGroup(attemptId).stream()
                .map(examMapper::toAttemptGroupScoreResponse)
                .collect(Collectors.toList());

        boolean hasThresholdFailed = groupScores.stream().anyMatch(g -> !g.isGroupPassed());

        Map<Long, StudentAnswer> studentAnswerMap = studentAnswerRepository
                .findByAttemptIdWithDetails(attemptId).stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Function.identity(), (e1, e2) -> e1));

        List<ReviewSectionResponse> reviewSections = attempt.getPublishedVersionId() != null
                ? snapshotReviewSections(attempt, studentAnswerMap)
                : liveReviewSections(attempt, exam, studentAnswerMap);

        ExamReviewResponse review = examMapper.toReviewResponse(attempt);
        if (attempt.getAttemptType() == AttemptType.PRACTICE_SECTION) {
            review.setTotalScaledMaxScore(60);
            review.setOverallPassingScore(30);
            for (var gs : groupScores) {
                if (ScoreGroupCode.LANGUAGE_KNOWLEDGE_READING.equals(gs.getGroupCode())) {
                    gs.setMaxScaledScore(60);
                    gs.setMinRequiredScore(19);
                }
            }
        }
        review.setHasThresholdFailed(hasThresholdFailed);
        review.setGroupScores(groupScores);
        review.setSections(reviewSections);
        return review;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExamAttemptHistoryResponse> getMyHistory(Long userId, Pageable pageable) {
        Page<ExamAttempt> attempts = examAttemptRepository
                .findByUserIdAndDeletedFalseOrderByStartedAtDesc(userId, pageable);

        return attempts.map(a -> {
            var groupScores = attemptGroupScoreRepository.findByAttemptIdWithScoreGroup(a.getId()).stream()
                    .map(examMapper::toAttemptGroupScoreResponse)
                    .collect(Collectors.toList());

            ExamAttemptHistoryResponse hist = examMapper.toAttemptHistoryResponse(a);
            hist.setGroupScores(groupScores);
            return hist;
        });
    }

    private ExamAttempt getAuthorizedAttempt(Long attemptId, Long userId) {
        ExamAttempt attempt = examAttemptRepository.findByIdAndDeletedFalse(attemptId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_ATTEMPT_NOT_FOUND));

        if (!attempt.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.EXAM_ATTEMPT_FORBIDDEN);
        }
        return attempt;
    }

    // ============================================================
    // SNAPSHOT (đề gắn khóa đã duyệt)
    // ============================================================

    /** 1 câu cần chấm: id câu, kỹ năng, nhóm điểm của ca, điểm; correctOptionIds null = chấm theo entity. */
    private record GradeItem(Long questionId, SkillType skill, ExamScoreGroup sectionGroup,
                             BigDecimal points, Set<Long> correctOptionIds) {
    }

    private List<GradeItem> liveGradeItems(ExamAttempt attempt, Exam exam) {
        List<ExamSectionQuestion> allSectionQuestions;
        if (attempt.getAttemptType() == AttemptType.PRACTICE_SECTION && attempt.getSection() != null) {
            allSectionQuestions = examSectionQuestionRepository
                    .findBySectionIdWithQuestionDetails(attempt.getSection().getId());
        } else {
            allSectionQuestions = examSectionQuestionRepository
                    .findByExamIdWithQuestionDetails(exam.getId());
        }
        return allSectionQuestions.stream()
                .map(sq -> new GradeItem(sq.getQuestion().getId(), sq.getQuestion().getSkillType(),
                        sq.getSection().getScoreGroup(), sq.getPoints(), null))
                .toList();
    }

    private List<GradeItem> snapshotGradeItems(ExamAttempt attempt, Exam exam, ExamSnapshot snapshot) {
        Map<Long, ExamScoreGroup> groupsById = exam.getScoreGroups().stream()
                .collect(Collectors.toMap(ExamScoreGroup::getId, Function.identity(), (a, b) -> a));
        List<GradeItem> items = new ArrayList<>();
        for (ExamSnapshot.Section s : attemptSections(attempt, snapshot)) {
            ExamScoreGroup sectionGroup = s.getScoreGroupId() != null ? groupsById.get(s.getScoreGroupId()) : null;
            for (ExamSnapshot.SectionQuestion q : s.getQuestions()) {
                items.add(new GradeItem(q.getQuestionId(), q.getSkillType(), sectionGroup,
                        q.getPoints() != null ? q.getPoints() : BigDecimal.ONE, q.getCorrectOptionIds()));
            }
        }
        return items;
    }

    private ExamSnapshot requireSnapshot(ExamAttempt attempt) {
        return examSnapshotService.find(attempt.getPublishedVersionId())
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));
    }

    /** Ca thi của lượt thi: luyện 1 ca → ca đó, thi cả đề → mọi ca. */
    private List<ExamSnapshot.Section> attemptSections(ExamAttempt attempt, ExamSnapshot snapshot) {
        if (attempt.getAttemptType() == AttemptType.PRACTICE_SECTION && attempt.getSection() != null) {
            Long sectionId = attempt.getSection().getId();
            return snapshot.getSections().stream().filter(s -> sectionId.equals(s.getId())).toList();
        }
        return snapshot.getSections();
    }

    private List<ExamSnapshot.SectionQuestion> attemptQuestions(ExamAttempt attempt, ExamSnapshot snapshot) {
        return attemptSections(attempt, snapshot).stream().flatMap(s -> s.getQuestions().stream()).toList();
    }

    private ExamTakingResponse startFromSnapshot(User user, Exam exam, StartExamRequest request) {
        ExamSnapshot snapshot = examSnapshotService.find(exam.getPublishedVersionId())
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        List<ExamSnapshot.Section> sections = snapshot.getSections();
        ExamSection targetSection = null;
        Integer snapshotDuration = snapshot.getSummary() != null ? snapshot.getSummary().getTotalDurationMinutes() : null;
        int targetDurationMinutes = snapshotDuration != null ? snapshotDuration : exam.getTotalDurationMinutes();

        if (request.getAttemptType() == AttemptType.PRACTICE_SECTION) {
            if (request.getSectionId() == null) {
                throw new AppException(ErrorCode.EXAM_SECTION_NOT_FOUND);
            }
            ExamSnapshot.Section section = sections.stream()
                    .filter(s -> request.getSectionId().equals(s.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.EXAM_SECTION_NOT_FOUND));
            targetSection = examSectionRepository.getReferenceById(section.getId());
            targetDurationMinutes = section.getDurationMinutes() != null ? section.getDurationMinutes() : 30;
            sections = List.of(section);
        }

        ExamAttempt attempt = examAttemptRepository.save(ExamAttempt.builder()
                .user(user)
                .exam(exam)
                .attemptType(request.getAttemptType())
                .section(targetSection)
                .targetDurationMinutes(targetDurationMinutes)
                .startedAt(Instant.now())
                .status(AttemptStatus.IN_PROGRESS)
                .publishedVersionId(exam.getPublishedVersionId())
                .build());

        List<TakingSectionResponse> sectionResponses = sections.stream()
                .map(s -> TakingSectionResponse.builder()
                        .id(s.getId())
                        .sessionType(s.getSessionType())
                        .sectionTitle(s.getSectionTitle())
                        .audioUrl(s.getAudioUrl())
                        .orderIndex(s.getOrderIndex())
                        .durationMinutes(s.getDurationMinutes())
                        .questions(s.getQuestions().stream().map(ExamSnapshot.SectionQuestion::getTaking)
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        ExamTakingResponse takingResp = examMapper.toTakingResponse(attempt);
        takingResp.setSections(sectionResponses);
        return takingResp;
    }

    private List<ReviewSectionResponse> snapshotReviewSections(ExamAttempt attempt,
                                                               Map<Long, StudentAnswer> studentAnswerMap) {
        return attemptSections(attempt, requireSnapshot(attempt)).stream()
                .map(s -> ReviewSectionResponse.builder()
                        .id(s.getId())
                        .sessionType(s.getSessionType())
                        .sectionTitle(s.getSectionTitle())
                        .audioUrl(s.getAudioUrl())
                        .orderIndex(s.getOrderIndex())
                        .durationMinutes(s.getDurationMinutes())
                        .questions(s.getQuestions().stream()
                                .map(q -> {
                                    ReviewQuestionResponse review = q.getReview();
                                    review.setStudentAnswer(examViewBuilder.toStudentAnswerResponse(
                                            studentAnswerMap.get(q.getQuestionId())));
                                    return review;
                                })
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<ReviewSectionResponse> liveReviewSections(ExamAttempt attempt, Exam exam,
                                                           Map<Long, StudentAnswer> studentAnswerMap) {
        List<ExamSection> sections;
        if (attempt.getAttemptType() == AttemptType.PRACTICE_SECTION && attempt.getSection() != null) {
            sections = List.of(attempt.getSection());
        } else {
            sections = examSectionRepository.findByExamIdAndDeletedFalseOrderByOrderIndexAsc(exam.getId());
        }

        return sections.stream()
                .map(s -> {
                    var sectionQuestions = examSectionQuestionRepository
                            .findBySectionIdWithQuestionDetails(s.getId());

                    var reviewQuestions = sectionQuestions.stream()
                            .map(sq -> examViewBuilder.toReviewQuestionResponse(sq.getQuestion(), sq.getQuestionNumber(),
                                    studentAnswerMap.get(sq.getQuestion().getId())))
                            .collect(Collectors.toList());

                    return ReviewSectionResponse.builder()
                            .id(s.getId())
                            .sessionType(s.getSessionType())
                            .sectionTitle(s.getSectionTitle())
                            .audioUrl(s.getAudioUrl())
                            .orderIndex(s.getOrderIndex())
                            .durationMinutes(s.getDurationMinutes())
                            .questions(reviewQuestions)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
