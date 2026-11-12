package com.jp.elearningjp.service.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jp.elearningjp.repository.curriculum.CourseTeacherRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.notification.NotificationService;
import com.jp.elearningjp.shared.constants.PredefinedRole;
import com.jp.elearningjp.shared.enums.NotificationType;
import com.jp.elearningjp.shared.event.curriculum.LessonReviewEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Objects;

/**
 * Thông báo cho luồng duyệt theo bài (docs/lesson-review-design.md mục 6):
 * <ul>
 *   <li>SUBMITTED → mọi ADMIN, {@code LESSON_SUBMITTED} — 1 thông báo cho cả lượt gửi nhiều bài;</li>
 *   <li>WITHDRAWN → mọi ADMIN, {@code LESSON_WITHDRAWN} (hàng chờ cập nhật);</li>
 *   <li>APPROVED / REJECTED → giáo viên đã gửi, {@code LESSON_APPROVED} / {@code LESSON_REJECTED} (kèm lý do);</li>
 *   <li>SAVED (ADMIN tự lưu bài) → chưa gửi gì; thông báo nháp cũ ({@code DRAFT_STALE}) làm ở phase sau.</li>
 * </ul>
 * Chạy AFTER_COMMIT, lỗi từng người nhận chỉ log. Không báo cho người thao tác.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonReviewNotificationListener {

    static final String ADMIN_QUEUE_LINK = "/admin/course-reviews";

    UserRepository userRepository;
    CourseTeacherRepository courseTeacherRepository;
    NotificationService notificationService;
    ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLessonReview(LessonReviewEvent event) {
        try {
            List<Long> recipients;
            NotificationType type;
            String link;
            switch (event.getAction()) {
                case SUBMITTED, WITHDRAWN -> {
                    recipients = userRepository.findActiveUserIdsByRole(PredefinedRole.ADMIN_ROLE);
                    type = event.getAction() == LessonReviewEvent.Action.SUBMITTED
                            ? NotificationType.LESSON_SUBMITTED : NotificationType.LESSON_WITHDRAWN;
                    link = ADMIN_QUEUE_LINK;
                }
                case APPROVED, REJECTED -> {
                    recipients = event.getSubmitterId() != null ? List.of(event.getSubmitterId()) : List.of();
                    type = event.getAction() == LessonReviewEvent.Action.APPROVED
                            ? NotificationType.LESSON_APPROVED : NotificationType.LESSON_REJECTED;
                    link = "/teacher/courses/" + event.getCourseId();
                }
                case DISCARDED -> {
                    // Phần giáo viên soạn chưa duyệt vừa bị bỏ → báo mọi giáo viên được giao
                    recipients = courseTeacherRepository.findTeacherIdsByCourseId(event.getCourseId());
                    type = NotificationType.LESSON_DRAFT_DISCARDED;
                    link = "/teacher/courses/" + event.getCourseId() + "/lessons/" + event.getLessonIds().get(0);
                }
                default -> {
                    return;
                }
            }
            int sent = send(recipients, event, type, buildTitle(event), buildBody(event), link);
            log.info("[Notification] lesson review {} (course {}, {} lesson(s)) -> notified {} user(s)",
                    event.getAction(), event.getCourseId(), event.getLessonIds().size(), sent);
        } catch (Exception ex) {
            log.warn("[Notification] lesson review notify failed (course {}, {}): {}",
                    event.getCourseId(), event.getAction(), ex.getMessage());
        }
    }

    private int send(List<Long> recipients, LessonReviewEvent event, NotificationType type,
                     String title, String body, String link) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("courseId", event.getCourseId());
        data.put("action", event.getAction().name());
        data.put("link", link);
        ArrayNode lessonIds = data.putArray("lessonIds");
        event.getLessonIds().forEach(lessonIds::add);
        int sent = 0;
        for (Long userId : recipients) {
            if (userId == null || Objects.equals(userId, event.getActorId())) continue;
            try {
                notificationService.create(userId, type, title, body, data);
                sent++;
            } catch (Exception ex) {
                log.warn("[Notification] lesson review notify failed -> user {}: {}", userId, ex.getMessage());
            }
        }
        return sent;
    }

    static String buildTitle(LessonReviewEvent event) {
        String course = "\"" + event.getCourseTitle() + "\"";
        String lessons = lessonsLabel(event);
        return switch (event.getAction()) {
            case SUBMITTED -> lessons + " của khóa " + course + " chờ duyệt";
            case WITHDRAWN -> "Đã rút yêu cầu duyệt " + lessons + " (khóa " + course + ")";
            case APPROVED -> lessons + " (khóa " + course + ") đã được duyệt";
            case REJECTED -> lessons + " (khóa " + course + ") chưa được duyệt";
            case SAVED -> "Quản trị viên đã cập nhật " + lessons + " (khóa " + course + ")";
            case DISCARDED -> "Quản trị viên đã bỏ thay đổi chưa duyệt của " + lessons + " (khóa " + course + ")";
        };
    }

    static String buildBody(LessonReviewEvent event) {
        String note = event.getNote();
        return switch (event.getAction()) {
            case SUBMITTED -> note != null ? "Ghi chú của giáo viên: " + note : "Giáo viên vừa gửi bài học lên duyệt.";
            case WITHDRAWN -> "Giáo viên đã rút yêu cầu duyệt để sửa tiếp bài học.";
            case APPROVED -> event.isLiveChangedWhilePublished()
                    ? "Học viên đã thấy nội dung mới của bài học."
                    : "Khóa học chưa xuất bản — học viên sẽ thấy khi quản trị viên xuất bản.";
            case REJECTED -> note != null ? "Lý do: " + note : "Vui lòng xem lại bài học và gửi duyệt lại.";
            case SAVED -> "Quản trị viên đã đưa bản nháp của bài lên bản học viên đang học.";
            case DISCARDED -> "Bản nháp của bài đã quay về đúng bản học viên đang học. Phần bạn chưa lưu trong form "
                    + "(nếu có) vẫn được giữ để chép lại.";
        };
    }

    /** 1 bài: "Bài \"Tên\""; nhiều bài: "3 bài học". */
    static String lessonsLabel(LessonReviewEvent event) {
        List<String> titles = event.getLessonTitles();
        return titles.size() == 1 ? "Bài \"" + titles.get(0) + "\"" : titles.size() + " bài học";
    }
}
