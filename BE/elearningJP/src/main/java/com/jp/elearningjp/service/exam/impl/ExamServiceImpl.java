package com.jp.elearningjp.service.exam.impl;

import com.jp.elearningjp.dto.response.exam.ExamDetailResponse;
import com.jp.elearningjp.dto.response.exam.ExamSectionSummaryResponse;
import com.jp.elearningjp.dto.response.exam.ExamSummaryResponse;
import com.jp.elearningjp.entity.exam.Exam;
import com.jp.elearningjp.entity.exam.ExamSection;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.ExamMapper;
import com.jp.elearningjp.dto.response.exam.LeaderboardItemResponse;
import com.jp.elearningjp.dto.response.exam.LeaderboardResponse;
import com.jp.elearningjp.repository.exam.ExamAttemptRepository;
import com.jp.elearningjp.repository.exam.ExamRepository;
import com.jp.elearningjp.repository.exam.ExamSectionQuestionRepository;
import com.jp.elearningjp.service.exam.ExamService;
import com.jp.elearningjp.shared.enums.ExamType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamSectionQuestionRepository examSectionQuestionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamMapper examMapper;

    @Override
    public Page<ExamSummaryResponse> getPublishedExams(JlptLevel level, ExamType examType, Pageable pageable) {
        Page<Exam> exams;
        if (level != null && examType != null) {
            exams = examRepository.findByJlptLevelAndExamTypeAndPublishedTrueAndDeletedFalse(level, examType, pageable);
        } else if (level != null) {
            exams = examRepository.findByJlptLevelAndPublishedTrueAndDeletedFalse(level, pageable);
        } else {
            exams = examRepository.findByPublishedTrueAndDeletedFalse(pageable);
        }

        return exams.map(examMapper::toSummaryResponse);
    }

    @Override
    public ExamDetailResponse getExamDetail(Long examId) {
        Exam exam = examRepository.findByIdWithSectionsAndScoreGroups(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        if (!exam.isPublished()) {
            throw new AppException(ErrorCode.EXAM_NOT_PUBLISHED);
        }

        var scoreGroupResponses = exam.getScoreGroups().stream()
                .map(examMapper::toScoreGroupResponse)
                .collect(Collectors.toList());

        var sectionResponses = exam.getSections().stream()
                .filter(s -> !s.isDeleted())
                .collect(Collectors.toMap(
                        ExamSection::getId,
                        s -> s,
                        (existing, replacement) -> existing,
                        java.util.LinkedHashMap::new
                ))
                .values().stream()
                .map(s -> {
                    int questionCount = examSectionQuestionRepository
                            .findBySectionIdAndDeletedFalseOrderByQuestionNumberAsc(s.getId()).size();
                    String title = formatSectionTitle(s);
                    ExamSectionSummaryResponse resp = examMapper.toSectionSummaryResponse(s);
                    resp.setSectionTitle(title);
                    resp.setQuestionCount(questionCount);
                    return resp;
                })
                .collect(Collectors.toList());

        ExamDetailResponse detail = examMapper.toDetailResponse(exam);
        detail.setScoreGroups(scoreGroupResponses);
        detail.setSections(sectionResponses);
        return detail;
    }

    @Override
    public java.util.List<ExamSummaryResponse> getPublishedCourseExams(Long courseId) {
        return examRepository.findByCourseIdAndPublishedTrueAndDeletedFalse(courseId).stream()
                .map(examMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    private String formatSectionTitle(ExamSection s) {
        String viDesc = switch (s.getSessionType()) {
            case VOCABULARY -> "Kiến thức ngôn ngữ (Từ vựng)";
            case GRAMMAR_READING -> "Ngữ pháp & Đọc hiểu";
            case LISTENING -> "Nghe hiểu";
            case KNOWLEDGE_READING -> "Kiến thức ngôn ngữ & Đọc hiểu";
        };
        if (s.getSectionTitle() != null && s.getSectionTitle().contains(viDesc)) {
            return s.getSectionTitle();
        }
        return "Ca " + s.getOrderIndex() + ": " + viDesc + " - " + s.getSectionTitle();
    }

    @Override
    public LeaderboardResponse getLeaderboard(JlptLevel level, String timeframe, Long currentUserId) {
        JlptLevel targetLevel = level != null ? level : JlptLevel.N5;
        Instant since = null;
        if ("WEEK".equalsIgnoreCase(timeframe)) {
            since = Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
        } else if ("MONTH".equalsIgnoreCase(timeframe)) {
            since = Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);
        }

        var attempts = (since != null)
                ? examAttemptRepository.findLeaderboardAttemptsWithSince(
                        com.jp.elearningjp.shared.enums.AttemptStatus.SUBMITTED,
                        targetLevel,
                        since)
                : examAttemptRepository.findLeaderboardAttemptsAllTime(
                        com.jp.elearningjp.shared.enums.AttemptStatus.SUBMITTED,
                        targetLevel);

        // Group by User, lấy bài thi có điểm cao nhất của mỗi user
        java.util.Map<Long, com.jp.elearningjp.entity.exam.ExamAttempt> bestAttemptByUser = new java.util.LinkedHashMap<>();
        for (var att : attempts) {
            Long uId = att.getUser().getId();
            if (!bestAttemptByUser.containsKey(uId)) {
                bestAttemptByUser.put(uId, att);
            }
        }

        java.util.List<LeaderboardItemResponse> items = new java.util.ArrayList<>();
        int rank = 1;
        LeaderboardItemResponse myRank = null;

        for (var entry : bestAttemptByUser.values()) {
            boolean isCur = currentUserId != null && currentUserId.equals(entry.getUser().getId());
            var item = LeaderboardItemResponse.builder()
                    .rank(rank)
                    .userId(entry.getUser().getId())
                    .fullName(entry.getUser().getFullName())
                    .avatarUrl(entry.getUser().getAvatarUrl())
                    .score(entry.getTotalScaledScore() != null ? entry.getTotalScaledScore() : 0)
                    .maxScore(entry.getExam() != null ? entry.getExam().getTotalScaledMaxScore() : 180)
                    .jlptLevel(entry.getExam() != null ? entry.getExam().getJlptLevel() : targetLevel)
                    .examTitle(entry.getExam() != null ? entry.getExam().getTitle() : "JLPT Mock Test")
                    .timeSpentSeconds(entry.getTimeSpentSeconds())
                    .submittedAt(entry.getSubmittedAt())
                    .isCurrentUser(isCur)
                    .build();

            items.add(item);
            if (isCur && myRank == null) {
                myRank = item;
            }
            rank++;
        }

        return LeaderboardResponse.builder()
                .rankings(items)
                .myRank(myRank)
                .totalParticipants((long) items.size())
                .build();
    }
}
