package com.jp.elearningjp.service.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jp.elearningjp.service.notification.NotificationService;
import com.jp.elearningjp.shared.enums.NotificationType;
import com.jp.elearningjp.shared.event.curriculum.CourseTeacherAssignmentChangedEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gửi thông báo {@code COURSE_TEACHER_ASSIGNMENT} cho giáo viên vừa được giao / bị gỡ khỏi khóa học.
 *
 * <p>Chạy AFTER_COMMIT: không báo cho thao tác bị rollback. Lỗi gửi thông báo chỉ log,
 * không làm hỏng thao tác phân công.</p>
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseTeacherAssignmentNotificationListener {

    NotificationService notificationService;
    ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAssignmentChanged(CourseTeacherAssignmentChangedEvent event) {
        try {
            ObjectNode data = objectMapper.createObjectNode();
            data.put("courseId", event.getCourseId());
            data.put("assigned", event.isAssigned());
            if (event.isAssigned()) {
                data.put("link", "/teacher/courses/" + event.getCourseId());
            }
            notificationService.create(event.getTeacherId(), NotificationType.COURSE_TEACHER_ASSIGNMENT,
                    buildTitle(event), buildBody(event), data);
            log.info("[Notification] course {} teacher {} {} -> notified",
                    event.getCourseId(), event.getTeacherId(), event.isAssigned() ? "assigned" : "unassigned");
        } catch (Exception ex) {
            log.warn("[Notification] course teacher assignment notify failed -> user {}: {}",
                    event.getTeacherId(), ex.getMessage());
        }
    }

    static String buildTitle(CourseTeacherAssignmentChangedEvent event) {
        String name = "\"" + event.getCourseTitle() + "\"";
        return event.isAssigned()
                ? "Bạn được giao khóa học " + name
                : "Bạn đã được gỡ khỏi khóa học " + name;
    }

    static String buildBody(CourseTeacherAssignmentChangedEvent event) {
        return event.isAssigned()
                ? "Bạn có thể bắt đầu soạn bài học cho khóa này trong trang quản lý khóa học."
                : "Bạn không còn quyền chỉnh sửa nội dung của khóa học này.";
    }
}
