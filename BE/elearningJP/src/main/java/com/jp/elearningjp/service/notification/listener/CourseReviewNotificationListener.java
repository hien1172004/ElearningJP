package com.jp.elearningjp.service.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseTeacherRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.notification.NotificationService;
import com.jp.elearningjp.shared.constants.PredefinedRole;
import com.jp.elearningjp.shared.enums.NotificationType;
import com.jp.elearningjp.shared.event.curriculum.CourseReviewEvent;
import com.jp.elearningjp.shared.event.curriculum.CourseReviewEvent.Action;
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
 * Thông báo {@code COURSE_REVIEW} cho luồng duyệt khóa học:
 * <ul>
 *   <li>SUBMITTED / WITHDRAWN → mọi ADMIN (mở hàng chờ duyệt);</li>
 *   <li>APPROVED / REJECTED / PUBLISHED / UNPUBLISHED → giáo viên được giao (mở trang soạn khóa);</li>
 *   <li>APPROVED cho khóa vốn đang xuất bản → học viên đã đăng ký nhận "khóa có nội dung mới".</li>
 * </ul>
 * Chạy AFTER_COMMIT, lỗi từng người nhận chỉ log — không làm hỏng thao tác gốc. Không báo cho người thao tác.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseReviewNotificationListener {

    UserRepository userRepository;
    CourseTeacherRepository courseTeacherRepository;
    CourseEnrollmentRepository enrollmentRepository;
    NotificationService notificationService;
    ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCourseReview(CourseReviewEvent event) {
        try {
            boolean toAdmins = event.getAction() == Action.SUBMITTED || event.getAction() == Action.WITHDRAWN;
            List<Long> recipients = toAdmins
                    ? userRepository.findActiveUserIdsByRole(PredefinedRole.ADMIN_ROLE)
                    : courseTeacherRepository.findTeacherIdsByCourseId(event.getCourseId());
            String link = toAdmins ? "/admin/course-reviews" : "/teacher/courses/" + event.getCourseId();
            int sent = send(recipients, event, buildTitle(event), buildBody(event), link);

            if (event.getAction() == Action.APPROVED && event.isContentUpdatedWhilePublished()) {
                sent += send(enrollmentRepository.findActiveUserIdsByCourseId(event.getCourseId()), event,
                        "Khóa học \"" + event.getCourseTitle() + "\" có nội dung mới",
                        "Giáo viên vừa cập nhật nội dung khóa học bạn đã đăng ký. Vào học ngay nhé!",
                        "/courses/" + event.getCourseId());
            }
            log.info("[Notification] course {} review {} -> notified {} user(s)",
                    event.getCourseId(), event.getAction(), sent);
        } catch (Exception ex) {
            log.warn("[Notification] course review notify failed (course {}, {}): {}",
                    event.getCourseId(), event.getAction(), ex.getMessage());
        }
    }

    private int send(List<Long> recipients, CourseReviewEvent event, String title, String body, String link) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("courseId", event.getCourseId());
        data.put("action", event.getAction().name());
        data.put("link", link);
        int sent = 0;
        for (Long userId : recipients) {
            if (userId == null || Objects.equals(userId, event.getActorId())) continue;
            try {
                notificationService.create(userId, NotificationType.COURSE_REVIEW, title, body, data);
                sent++;
            } catch (Exception ex) {
                log.warn("[Notification] course review notify failed -> user {}: {}", userId, ex.getMessage());
            }
        }
        return sent;
    }

    static String buildTitle(CourseReviewEvent event) {
        String name = "\"" + event.getCourseTitle() + "\"";
        return switch (event.getAction()) {
            case SUBMITTED -> "Khóa học " + name + " chờ duyệt";
            case WITHDRAWN -> "Yêu cầu duyệt khóa học " + name + " đã được rút";
            case APPROVED -> "Khóa học " + name + " đã được duyệt và xuất bản";
            case REJECTED -> "Khóa học " + name + " chưa được duyệt";
            case PUBLISHED -> "Khóa học " + name + " đã được xuất bản";
            case UNPUBLISHED -> "Khóa học " + name + " đã chuyển về bản nháp";
        };
    }

    static String buildBody(CourseReviewEvent event) {
        String note = event.getNote();
        return switch (event.getAction()) {
            case SUBMITTED -> note != null ? "Ghi chú của giáo viên: " + note : "Giáo viên vừa gửi khóa học lên duyệt.";
            case WITHDRAWN -> "Giáo viên đã rút yêu cầu duyệt để sửa tiếp nội dung.";
            case APPROVED -> "Học viên đã thấy nội dung mới nhất của khóa học.";
            case REJECTED -> note != null ? "Lý do: " + note : "Vui lòng xem lại nội dung và gửi duyệt lại.";
            case PUBLISHED -> "Quản trị viên đã xuất bản khóa học bạn được giao.";
            case UNPUBLISHED -> "Quản trị viên đã ẩn khóa học khỏi học viên.";
        };
    }
}
