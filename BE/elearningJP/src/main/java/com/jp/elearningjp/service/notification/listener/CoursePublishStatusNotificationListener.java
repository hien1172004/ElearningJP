package com.jp.elearningjp.service.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.service.notification.NotificationService;
import com.jp.elearningjp.shared.enums.NotificationType;
import com.jp.elearningjp.shared.event.curriculum.CoursePublishStatusChangedEvent;
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
 * Gửi thông báo {@code COURSE_STATUS_CHANGED} tới mọi học viên đã đăng ký khi khóa học được
 * chuyển giữa bản nháp và xuất bản.
 *
 * <p>Chạy AFTER_COMMIT: không báo cho thao tác bị rollback. Mỗi người nhận một try/catch riêng —
 * lỗi gửi cho một người không chặn những người còn lại, cũng không làm hỏng thao tác xuất bản.</p>
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CoursePublishStatusNotificationListener {

    CourseEnrollmentRepository enrollmentRepository;
    NotificationService notificationService;
    ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCoursePublishStatusChanged(CoursePublishStatusChangedEvent event) {
        List<Long> recipients;
        try {
            recipients = enrollmentRepository.findActiveUserIdsByCourseId(event.getCourseId());
        } catch (Exception ex) {
            log.warn("[Notification] load enrolled users of course {} failed: {}", event.getCourseId(), ex.getMessage());
            return;
        }

        ObjectNode data = objectMapper.createObjectNode();
        data.put("courseId", event.getCourseId());
        data.put("published", event.isPublished());
        data.put("link", "/courses/" + event.getCourseId());
        String title = buildTitle(event);
        String body = buildBody(event);

        int sent = 0;
        for (Long userId : recipients) {
            // Không báo cho chính người thao tác (vd giáo viên tự đăng ký khóa của mình)
            if (userId == null || Objects.equals(userId, event.getChangedByUserId())) continue;
            try {
                notificationService.create(userId, NotificationType.COURSE_STATUS_CHANGED, title, body, data);
                sent++;
            } catch (Exception ex) {
                log.warn("[Notification] course status notify failed -> user {}: {}", userId, ex.getMessage());
            }
        }
        log.info("[Notification] course {} {} -> notified {} enrolled user(s)",
                event.getCourseId(), event.isPublished() ? "published" : "unpublished", sent);
    }

    static String buildTitle(CoursePublishStatusChangedEvent event) {
        String name = "\"" + event.getCourseTitle() + "\"";
        return event.isPublished()
                ? "Khóa học " + name + " đã được xuất bản"
                : "Khóa học " + name + " tạm chuyển về bản nháp";
    }

    static String buildBody(CoursePublishStatusChangedEvent event) {
        return event.isPublished()
                ? "Khóa học bạn đã đăng ký đang mở. Hãy tiếp tục học các bài mới nhé!"
                : "Giáo viên đang cập nhật nội dung khóa học. Bạn sẽ nhận được thông báo khi khóa học được xuất bản lại.";
    }
}
