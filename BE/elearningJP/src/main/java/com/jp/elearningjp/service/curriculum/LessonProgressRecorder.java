package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.shared.event.curriculum.LessonCompletedEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Điểm ghi tiến độ bài học dùng chung cho mọi luồng (xem bài, bắt đầu, hoàn thành, nộp quiz).
 *
 * <ul>
 *   <li>{@link #lockOrCreate}: tạo dòng progress nếu chưa có rồi khoá dòng tới hết transaction,
 *       nên 2 request đồng thời không còn cùng INSERT (vi phạm UNIQUE → 500) hay ghi đè nhau.</li>
 *   <li>{@link #onLessonCompleted}: tác dụng phụ khi bài vừa chuyển sang COMPLETED — phát
 *       {@link LessonCompletedEvent} (XP, streak) và tự hoàn thành khóa khi đạt 100%.
 *       Caller chỉ gọi ở lần đầu hoàn thành để không cộng XP trùng.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonProgressRecorder {

    UserLessonProgressRepository progressRepository;
    EnrollmentService enrollmentService;
    ApplicationEventPublisher eventPublisher;

    /**
     * Lấy progress (chưa xoá) của user cho bài, tạo mới ở trạng thái NOT_STARTED nếu chưa có.
     * Dòng bị khoá (SELECT ... FOR UPDATE) tới khi transaction của caller kết thúc.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UserLessonProgress lockOrCreate(Long userId, Long lessonId) {
        progressRepository.insertIfAbsent(userId, lessonId);
        return progressRepository.findActiveForUpdate(userId, lessonId)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy progress vừa tạo cho user " + userId + ", lesson " + lessonId));
    }

    /**
     * Gọi ngay sau khi lưu progress vừa chuyển sang COMPLETED.
     *
     * @return % tiến độ khóa học sau khi hoàn thành
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int onLessonCompleted(Long userId, Lesson lesson, BigDecimal bestScore, boolean hasQuiz) {
        Long courseId = lesson.getCourse().getId();
        progressRepository.flush(); // query % là native SQL — phải thấy trạng thái COMPLETED vừa ghi
        int percent = progressRepository.calculateCourseProgressPercent(userId, courseId);

        eventPublisher.publishEvent(new LessonCompletedEvent(this,
                userId, lesson.getId(), courseId, bestScore, hasQuiz, percent));

        if (percent >= 100) {
            enrollmentService.markAsCompletedIfFull(userId, courseId);
        }
        log.info("[PROGRESS] User id={} completed lesson id={}, course id={} at {}%",
                userId, lesson.getId(), courseId, percent);
        return percent;
    }
}
