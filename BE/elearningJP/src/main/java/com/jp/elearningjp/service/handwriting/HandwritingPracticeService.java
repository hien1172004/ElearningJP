package com.jp.elearningjp.service.handwriting;

import com.jp.elearningjp.dto.request.handwriting.HandwritingPracticeLogRequest;
import com.jp.elearningjp.dto.response.handwriting.HandwritingPracticeLogResponse;
import com.jp.elearningjp.dto.response.handwriting.HandwritingPracticeSummaryResponse;
import com.jp.elearningjp.dto.response.handwriting.HandwritingPracticeSummaryResponse.ModeStat;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.handwriting.HandwritingPracticeLog;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.handwriting.HandwritingPracticeLogRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Lưu & tổng hợp điểm luyện viết Kanji.
 *
 * <p>Điểm Hanzi-Writer (GUIDED) và AI (FREEHAND), chuột (MOUSE) và bằng tay (AIR) chấm theo
 * thang/ngưỡng khác nhau nên luôn tổng hợp tách theo cặp (practiceMode, inputMode).</p>
 */
@Service
@RequiredArgsConstructor
public class HandwritingPracticeService {

    /** Ngưỡng đạt — trùng mốc "xanh" (>= 80) ở màn kết quả FE. */
    private static final BigDecimal PASS_SCORE = BigDecimal.valueOf(80);

    private final HandwritingPracticeLogRepository logRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    @Transactional
    public HandwritingPracticeLogResponse save(Long userId, HandwritingPracticeLogRequest req) {
        Lesson lesson = null;
        if (req.getLessonId() != null) {
            lesson = lessonRepository.findByIdAndDeletedFalse(req.getLessonId())
                    .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        }
        String character = req.getCharacter().trim();
        BigDecimal score = req.getTotalScore().setScale(2, RoundingMode.HALF_UP);

        // Điểm cao nhất trước khi lưu (cùng chế độ + nguồn nhập) để báo "kỷ lục mới"
        List<HandwritingPracticeLog> before = logRepository.findByUserAndCharacter(userId, character, req.getLessonId());
        BigDecimal previousBest = before.stream()
                .filter(l -> l.getPracticeMode() == req.getPracticeMode() && l.getInputMode() == req.getInputMode())
                .map(HandwritingPracticeLog::getTotalScore)
                .max(Comparator.naturalOrder())
                .orElse(null);

        HandwritingPracticeLog log = logRepository.save(HandwritingPracticeLog.builder()
                .user(userRepository.getReferenceById(userId))
                .lesson(lesson)
                .character(character)
                .practiceMode(req.getPracticeMode())
                .inputMode(req.getInputMode())
                .totalScore(score)
                .strokeCountExpected(req.getStrokeCountExpected())
                .strokeCountDrawn(req.getStrokeCountDrawn())
                .totalMistakes(req.getTotalMistakes())
                .passed(score.compareTo(PASS_SCORE) >= 0)
                .detailJson(req.getDetail())
                .build());

        List<HandwritingPracticeLog> after = new ArrayList<>(before.size() + 1);
        after.add(log);
        after.addAll(before);

        return HandwritingPracticeLogResponse.builder()
                .logId(log.getId())
                .passed(log.isPassed())
                .newBest(previousBest == null || score.compareTo(previousBest) > 0)
                .summary(buildSummary(character, req.getLessonId(), after))
                .build();
    }

    @Transactional(readOnly = true)
    public HandwritingPracticeSummaryResponse summary(Long userId, String character, Long lessonId) {
        String ch = character.trim();
        return buildSummary(ch, lessonId, logRepository.findByUserAndCharacter(userId, ch, lessonId));
    }

    /** logs phải sắp xếp mới nhất trước. */
    private HandwritingPracticeSummaryResponse buildSummary(String character, Long lessonId,
                                                            List<HandwritingPracticeLog> logs) {
        Map<String, ModeStat> stats = new LinkedHashMap<>();
        for (HandwritingPracticeLog l : logs) {
            String key = l.getPracticeMode() + "_" + l.getInputMode();
            ModeStat s = stats.get(key);
            if (s == null) {
                // Lần đầu gặp = lần mới nhất của cặp chế độ này
                s = ModeStat.builder()
                        .practiceMode(l.getPracticeMode())
                        .inputMode(l.getInputMode())
                        .attempts(0)
                        .bestScore(l.getTotalScore())
                        .lastScore(l.getTotalScore())
                        .lastPracticedAt(l.getCreatedAt())
                        .build();
                stats.put(key, s);
            }
            s.setAttempts(s.getAttempts() + 1);
            if (l.getTotalScore().compareTo(s.getBestScore()) > 0) s.setBestScore(l.getTotalScore());
            if (l.isPassed()) s.setPassed(true);
        }
        return HandwritingPracticeSummaryResponse.builder()
                .character(character)
                .lessonId(lessonId)
                .totalAttempts(logs.size())
                .stats(new ArrayList<>(stats.values()))
                .build();
    }
}
