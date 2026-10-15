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

    @Override
    public ExamTakingResponse startExam(Long examId, Long userId, StartExamRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Exam exam = examRepository.findByIdWithSectionsAndScoreGroups(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        if (!exam.isPublished()) {
            throw new AppException(ErrorCode.EXAM_NOT_PUBLISHED);
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
                            .map(sq -> toTakingQuestionResponse(sq.getQuestion(), sq.getQuestionNumber()))
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

        Question question = questionRepository.findByIdWithOptionsAndPassage(request.getQuestionId())
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_QUESTION_NOT_FOUND));

        QuestionOption selectedOption = null;
        if (request.getSelectedOptionId() != null) {
            selectedOption = question.getOptions().stream()
                    .filter(o -> o.getId().equals(request.getSelectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.EXAM_OPTION_NOT_FOUND));
        }

        StudentAnswer answer = studentAnswerRepository
                .findByAttemptIdAndQuestionIdAndDeletedFalse(attemptId, request.getQuestionId())
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

        if (request != null && request.getAnswers() != null) {
            for (SaveAnswerRequest ansReq : request.getAnswers()) {
                saveAnswer(attemptId, userId, ansReq);
            }
        }

        Exam exam = examRepository.findByIdWithSectionsAndScoreGroups(attempt.getExam().getId())
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        int timeSpentSeconds = (int) Duration.between(attempt.getStartedAt(), Instant.now()).getSeconds();

        List<ExamSectionQuestion> allSectionQuestions;
        if (attempt.getAttemptType() == AttemptType.PRACTICE_SECTION && attempt.getSection() != null) {
            allSectionQuestions = examSectionQuestionRepository
                    .findBySectionIdWithQuestionDetails(attempt.getSection().getId());
        } else {
            allSectionQuestions = examSectionQuestionRepository
                    .findByExamIdWithQuestionDetails(exam.getId());
        }

        Map<Long, ExamScoreGroup> questionToScoreGroupMap = new HashMap<>();
        for (ExamSectionQuestion sq : allSectionQuestions) {
            ExamScoreGroup sg = sq.getSection().getScoreGroup();
            if (sg != null) {
                questionToScoreGroupMap.put(sq.getQuestion().getId(), sg);
            }
        }

        List<StudentAnswer> studentAnswers = studentAnswerRepository.findByAttemptIdWithDetails(attemptId);
        Map<Long, StudentAnswer> answerMap = studentAnswers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Function.identity(), (e1, e2) -> e1));

        BigDecimal totalRawScore = BigDecimal.ZERO;
        Map<Long, Integer> groupRawCorrectMap = new HashMap<>();
        Map<Long, Integer> groupRawTotalMap = new HashMap<>();

        for (ExamSectionQuestion sq : allSectionQuestions) {
            Long qId = sq.getQuestion().getId();
            ExamScoreGroup sg = questionToScoreGroupMap.get(qId);
            Long sgId = sg != null ? sg.getId() : -1L;

            groupRawTotalMap.put(sgId, groupRawTotalMap.getOrDefault(sgId, 0) + 1);

            StudentAnswer ans = answerMap.get(qId);
            if (ans != null && ans.getSelectedOption() != null) {
                boolean isCorrect = ans.getSelectedOption().isCorrect();
                ans.setCorrect(isCorrect);
                BigDecimal points = isCorrect ? sq.getPoints() : BigDecimal.ZERO;
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

        Collection<ExamScoreGroup> scoreGroupsToCalculate = exam.getScoreGroups();
        if (attempt.getAttemptType() == AttemptType.PRACTICE_SECTION && attempt.getSection() != null) {
            if (attempt.getSection().getScoreGroup() != null) {
                scoreGroupsToCalculate = List.of(attempt.getSection().getScoreGroup());
            }
        }

        for (ExamScoreGroup sg : scoreGroupsToCalculate) {
            int rawCorrect = groupRawCorrectMap.getOrDefault(sg.getId(), 0);
            int rawTotal = groupRawTotalMap.getOrDefault(sg.getId(), 0);

            int scaledScore = 0;
            if (rawTotal > 0) {
                scaledScore = (int) Math.round(((double) rawCorrect / rawTotal) * sg.getMaxScaledScore());
            }

            boolean isGroupPassed = scaledScore >= sg.getMinRequiredScore();
            if (!isGroupPassed) {
                hasThresholdFailed = true;
            }

            totalScaledScore += scaledScore;

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

            groupScoreResponses.add(examMapper.toAttemptGroupScoreResponse(ags));
        }

        int passingThreshold = exam.getOverallPassingScore() != null ? exam.getOverallPassingScore() : 90;
        boolean isOverallPassed = totalScaledScore >= passingThreshold;
        boolean finalPassed = isOverallPassed && !hasThresholdFailed;

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(Instant.now());
        attempt.setTimeSpentSeconds(timeSpentSeconds);
        attempt.setTotalRawScore(totalRawScore);
        attempt.setTotalScaledScore(totalScaledScore);
        attempt.setPassed(finalPassed);
        examAttemptRepository.save(attempt);

        ExamResultResponse result = examMapper.toResultResponse(attempt);
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

        List<ExamSection> sections;
        if (attempt.getAttemptType() == AttemptType.PRACTICE_SECTION && attempt.getSection() != null) {
            sections = List.of(attempt.getSection());
        } else {
            sections = examSectionRepository.findByExamIdAndDeletedFalseOrderByOrderIndexAsc(exam.getId());
        }

        List<ReviewSectionResponse> reviewSections = sections.stream()
                .map(s -> {
                    var sectionQuestions = examSectionQuestionRepository
                            .findBySectionIdWithQuestionDetails(s.getId());

                    var reviewQuestions = sectionQuestions.stream()
                            .map(sq -> toReviewQuestionResponse(sq.getQuestion(), sq.getQuestionNumber(),
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

        ExamReviewResponse review = examMapper.toReviewResponse(attempt);
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

    private TakingQuestionResponse toTakingQuestionResponse(Question q, Integer questionNumber) {
        var options = q.getOptions().stream()
                .filter(o -> !o.isDeleted())
                .sorted(Comparator.comparing(QuestionOption::getOptionKey))
                .map(examMapper::toTakingOptionResponse)
                .collect(Collectors.toList());

        TakingPassageResponse passage = (q.getPassage() != null && !q.getPassage().isDeleted())
                ? examMapper.toTakingPassageResponse(q.getPassage()) : null;

        TakingQuestionResponse resp = examMapper.toTakingQuestionResponse(q);
        resp.setQuestionNumber(questionNumber);
        resp.setOptions(options);
        resp.setPassage(passage);
        return resp;
    }

    private ReviewQuestionResponse toReviewQuestionResponse(Question q, Integer questionNumber, StudentAnswer ans) {
        var options = q.getOptions().stream()
                .filter(o -> !o.isDeleted())
                .sorted(Comparator.comparing(QuestionOption::getOptionKey))
                .map(examMapper::toReviewOptionResponse)
                .collect(Collectors.toList());

        TakingPassageResponse passage = (q.getPassage() != null && !q.getPassage().isDeleted())
                ? examMapper.toTakingPassageResponse(q.getPassage()) : null;

        ReviewStudentAnswerResponse studentAnsResp = null;
        if (ans != null) {
            studentAnsResp = ReviewStudentAnswerResponse.builder()
                    .selectedOptionId(ans.getSelectedOption() != null ? ans.getSelectedOption().getId() : null)
                    .orderedOptionIds(ans.getOrderedOptionIds())
                    .correct(ans.isCorrect())
                    .pointsEarned(ans.getPointsEarned())
                    .audioPlayCount(ans.getAudioPlayCount())
                    .build();
        }

        ReviewQuestionResponse resp = examMapper.toReviewQuestionResponse(q);
        resp.setQuestionNumber(questionNumber);
        resp.setOptions(options);
        resp.setPassage(passage);
        resp.setStudentAnswer(studentAnsResp);
        return resp;
    }
}
