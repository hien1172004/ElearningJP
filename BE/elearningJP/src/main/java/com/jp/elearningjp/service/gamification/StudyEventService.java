package com.jp.elearningjp.service.gamification;

import com.jp.elearningjp.entity.gamification.StudyEvent;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.repository.gamification.StudyEventRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.enums.LookupTargetType;
import com.jp.elearningjp.shared.enums.StudyEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi su kien hoc tap vao bang append-only {@code study_events}.
 *
 * <p>Chay {@code REQUIRES_NEW} + try/catch de analytics khong bao gio lam fail
 * request chinh (neu bang co loi, chi log warn).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyEventService {

    private final StudyEventRepository studyEventRepository;
    private final UserRepository userRepository;

    /** Ghi 1 su kien tra tu dien. Kanji luu chuoi vao {@code sourceRef}. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLookup(Long userId, LookupTargetType targetType, Long targetId, String sourceRef, int xp) {
        log(StudyEventType.LOOKUP, userId, targetType, targetId, sourceRef, xp);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLessonComplete(Long userId, Long lessonId, int xp) {
        log(StudyEventType.LESSON_COMPLETE, userId, null, lessonId, null, xp);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logQuizSubmit(Long userId, Long lessonId, boolean passed, int xp) {
        log(StudyEventType.QUIZ_SUBMIT, userId, null, lessonId, passed ? "PASSED" : "FAILED", xp);
    }

    /** Heartbeat chi ghi thoi gian, khong tao row de tranh nhoe database. */
    public void logHeartbeat(Long userId, int seconds) {
        if (seconds <= 0) {
            return;
        }
        try {
            log(StudyEventType.HEARTBEAT, userId, null, null, null, 0);
        } catch (Exception ex) {
            log.warn("[StudyEventService] heartbeat log failed for user {}: {}", userId, ex.getMessage());
        }
    }

    private void log(StudyEventType type, Long userId, LookupTargetType targetType,
                     Long targetId, String sourceRef, int xp) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("[StudyEventService] user {} not found, skip log {}", userId, type);
                return;
            }
            studyEventRepository.save(StudyEvent.builder()
                    .user(user)
                    .eventType(type)
                    .targetType(targetType)
                    .targetId(targetId)
                    .sourceRef(sourceRef)
                    .xpAwarded(xp)
                    .build());
        } catch (Exception ex) {
            log.warn("[StudyEventService] log {} failed for user {}: {}", type, userId, ex.getMessage());
        }
    }
}