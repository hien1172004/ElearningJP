package com.jp.elearningjp.service.curriculum.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.shared.enums.DraftKind;
import com.jp.elearningjp.shared.event.curriculum.CourseContentChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Đẩy tín hiệu "nội dung khóa học vừa đổi" tới client đang mở khóa đó, qua STOMP topic
 * {@code /topic/courses/{courseId}/content}.
 *
 * <p>Giống {@code NotificationPublisher}: simple broker nằm trong bộ nhớ từng instance, nên phát
 * lên kênh Redis {@link #CHANNEL}; mọi instance nhận qua {@link CourseContentRedisSubscriber} và
 * gửi cho phiên của mình. Redis lỗi → gửi thẳng phiên cục bộ.</p>
 *
 * <p>Payload chỉ có id khóa, phạm vi và người thao tác — không chứa nội dung, nên topic không cần
 * phân quyền subscribe riêng. Client tự tải lại qua REST (vẫn kiểm quyền như thường).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseContentPublisher {

    public static final String CHANNEL = "course-content:push";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /** Gói tin gửi client (và trên kênh Redis). {@code draftKind}: nháp phía nào đổi (chỉ DRAFT; null = dùng chung). */
    public record ContentChangedMessage(Long courseId, CourseContentChangedEvent.Scope scope, Long actorId,
                                        DraftKind draftKind) {
    }

    public static String destination(Long courseId) {
        return "/topic/courses/" + courseId + "/content";
    }

    public void publish(Long courseId, CourseContentChangedEvent.Scope scope, Long actorId) {
        publish(courseId, scope, actorId, null);
    }

    public void publish(Long courseId, CourseContentChangedEvent.Scope scope, Long actorId, DraftKind draftKind) {
        if (courseId == null || scope == null) {
            return;
        }
        ContentChangedMessage message = new ContentChangedMessage(courseId, scope, actorId, draftKind);
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(message));
        } catch (Exception ex) {
            log.warn("[COURSE-CONTENT] Redis publish failed for course {}, delivering locally only: {}",
                    courseId, ex.getMessage());
            deliverLocal(message);
        }
    }

    /** Gửi cho phiên STOMP của instance này; lỗi broker chỉ log. */
    public void deliverLocal(ContentChangedMessage message) {
        try {
            messagingTemplate.convertAndSend(destination(message.courseId()), message);
        } catch (Exception ex) {
            log.warn("[COURSE-CONTENT] STOMP push failed for course {}: {}", message.courseId(), ex.getMessage());
        }
    }
}
