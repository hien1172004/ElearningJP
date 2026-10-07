package com.jp.elearningjp.shared.event.curriculum;

import lombok.Getter;

/**
 * Event phát ra khi user drop enrollment (hủy đăng ký course).
 */
@Getter
public class UserDroppedCourseEvent extends CurriculumEvent {

    private final Long userId;
    private final Long courseId;
    private final Long enrollmentId;

    public UserDroppedCourseEvent(Object source, Long userId, Long courseId, Long enrollmentId) {
        super(source);
        this.userId = userId;
        this.courseId = courseId;
        this.enrollmentId = enrollmentId;
    }
}