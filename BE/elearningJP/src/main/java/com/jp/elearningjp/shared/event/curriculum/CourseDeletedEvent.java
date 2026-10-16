package com.jp.elearningjp.shared.event.curriculum;

import lombok.Getter;

/**
 * Event phát ra khi Course bị soft-delete (kèm cascade lesson/enrollment/progress).
 * Consumer tương lai có thể: xóa khỏi search index, gửi thông báo cho user đã enroll,
 * thu hồi XP/badges liên quan, ...
 */
@Getter
public class CourseDeletedEvent extends CurriculumEvent {

    private final Long courseId;
    private final Long deletedByUserId;

    public CourseDeletedEvent(Object source, Long courseId, Long deletedByUserId) {
        super(source);
        this.courseId = courseId;
        this.deletedByUserId = deletedByUserId;
    }
}