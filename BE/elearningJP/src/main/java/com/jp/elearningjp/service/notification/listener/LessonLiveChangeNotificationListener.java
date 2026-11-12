package com.jp.elearningjp.service.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jp.elearningjp.repository.curriculum.LessonAutosaveRepository;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.service.notification.NotificationService;
import com.jp.elearningjp.shared.enums.NotificationType;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.event.curriculum.CourseReviewEvent;
import com.jp.elearningjp.shared.event.curriculum.LessonReviewEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Thông báo khi bản học viên đang học (bản live) của bài vừa đổi (docs/lesson-review-design.md mục 6):
 * <ul>
 *   <li>{@code DRAFT_STALE} → người còn bản chưa lưu (autosave) của bài: bản đó dựa trên nội dung cũ.
 *       Bản nháp của bài dùng chung nên "nháp riêng" của từng người chính là bản tự lưu. Áp dụng cho duyệt / lưu
 *       theo bài và lưu / duyệt cả khóa (mọi bài của khóa).</li>
 *   <li>{@code LESSON_UPDATED} → học viên đã hoàn thành bài, chỉ khi ADMIN đánh dấu "thay đổi lớn" và khóa đang
 *       xuất bản (học viên thấy nội dung mới ngay).</li>
 * </ul>
 * AFTER_COMMIT; lỗi từng người nhận chỉ log. Không báo cho người thao tác.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonLiveChangeNotificationListener {

    LessonAutosaveRepository autosaveRepository;
    UserLessonProgressRepository progressRepository;
    NotificationService notificationService;
    ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLessonLiveChanged(LessonReviewEvent event) {
        if (event.getAction() != LessonReviewEvent.Action.APPROVED && event.getAction() != LessonReviewEvent.Action.SAVED) {
            return;
        }
        try {
            String lessons = lessonsLabel(event.getLessonTitles());
            String link = event.getLessonIds().size() == 1
                    ? "/teacher/courses/" + event.getCourseId() + "/lessons/" + event.getLessonIds().get(0)
                    : "/teacher/courses/" + event.getCourseId();
            int stale = send(autosaveRepository.findUserIdsByLessonIds(event.getLessonIds()), event.getActorId(),
                    NotificationType.DRAFT_STALE,
                    lessons + " (khóa \"" + event.getCourseTitle() + "\") vừa được cập nhật",
                    "Bản học viên đang học đã đổi nên bản chưa lưu của bạn dựa trên nội dung cũ. Mở bài để xem bản mới "
                            + "nhất; phần chưa lưu vẫn được giữ để chép lại.",
                    data(event.getCourseId(), event.getLessonIds(), link));

            int learners = 0;
            if (event.isMajorChange() && event.isLiveChangedWhilePublished()) {
                for (Long lessonId : event.getLessonIds()) {
                    learners += send(progressRepository.findUserIdsByLessonIdAndStatus(lessonId, ProgressStatus.COMPLETED),
                            event.getActorId(), NotificationType.LESSON_UPDATED,
                            lessons + " có nội dung mới",
                            "Bài bạn đã học trong khóa \"" + event.getCourseTitle() + "\" vừa được cập nhật đáng kể. "
                                    + "Ôn lại để không bỏ lỡ nội dung mới nhé!",
                            data(event.getCourseId(), List.of(lessonId), "/courses/" + event.getCourseId()));
                }
            }
            log.info("[Notification] lesson live changed (course {}, {} lesson(s), {}) -> {} draft-stale, {} learner(s)",
                    event.getCourseId(), event.getLessonIds().size(), event.getAction(), stale, learners);
        } catch (Exception ex) {
            log.warn("[Notification] lesson live-change notify failed (course {}): {}", event.getCourseId(), ex.getMessage());
        }
    }

    /** Lưu / duyệt cả khóa: mọi bài có thể đổi → báo người còn bản chưa lưu của bài bất kỳ trong khóa. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCourseLiveChanged(CourseReviewEvent event) {
        if (event.getAction() != CourseReviewEvent.Action.APPROVED && event.getAction() != CourseReviewEvent.Action.SAVED) {
            return;
        }
        try {
            int stale = send(autosaveRepository.findUserIdsByCourseId(event.getCourseId()), event.getActorId(),
                    NotificationType.DRAFT_STALE,
                    "Khóa học \"" + event.getCourseTitle() + "\" vừa có phiên bản mới",
                    "Bản học viên đang học đã đổi nên các bản chưa lưu của bạn trong khóa dựa trên nội dung cũ. Mở bài "
                            + "để xem bản mới nhất; phần chưa lưu vẫn được giữ để chép lại.",
                    data(event.getCourseId(), List.of(), "/teacher/courses/" + event.getCourseId()));
            log.info("[Notification] course {} live changed ({}) -> {} draft-stale", event.getCourseId(),
                    event.getAction(), stale);
        } catch (Exception ex) {
            log.warn("[Notification] course live-change notify failed (course {}): {}", event.getCourseId(), ex.getMessage());
        }
    }

    private ObjectNode data(Long courseId, Collection<Long> lessonIds, String link) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("courseId", courseId);
        data.put("link", link);
        var ids = data.putArray("lessonIds");
        lessonIds.forEach(ids::add);
        return data;
    }

    private int send(List<Long> recipients, Long actorId, NotificationType type, String title, String body,
                     ObjectNode data) {
        Set<Long> unique = new LinkedHashSet<>(recipients);
        int sent = 0;
        for (Long userId : unique) {
            if (userId == null || Objects.equals(userId, actorId)) continue;
            try {
                notificationService.create(userId, type, title, body, data);
                sent++;
            } catch (Exception ex) {
                log.warn("[Notification] {} notify failed -> user {}: {}", type, userId, ex.getMessage());
            }
        }
        return sent;
    }

    /** 1 bài: "Bài \"Tên\""; nhiều bài: "3 bài học". */
    static String lessonsLabel(List<String> titles) {
        return titles.size() == 1 ? "Bài \"" + titles.get(0) + "\"" : titles.size() + " bài học";
    }
}
