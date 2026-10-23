package com.jp.elearningjp.service.gamification.listener;

import com.jp.elearningjp.service.gamification.StudyTrackerService;
import com.jp.elearningjp.service.gamification.XpService;
import com.jp.elearningjp.shared.event.curriculum.LessonCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Nghe {@link LessonCompletedEvent} de cong XP + cap nhat streak.
 *
 * <p>Chay {@code AFTER_COMMIT} nen:
 * <ul>
 *   <li>Transaction goc da commit thanh cong (khong cong XP cho lesson bi rollback)</li>
 *   <li>Khong chan response time cua {@code POST /lessons/{id}/complete}</li>
 * </ul></p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LessonCompletedGamificationListener {

    private final StudyTrackerService studyTrackerService;
    private final XpService xpService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLessonCompleted(LessonCompletedEvent event) {
        try {
            studyTrackerService.recordLessonComplete(
                    event.getUserId(), event.getLessonId(), event.getCourseId(),
                    xpService.getLessonXp());
        } catch (Exception ex) {
            log.warn("[Gamification] lesson complete XP failed for user {}: {}",
                    event.getUserId(), ex.getMessage());
        }
    }
}