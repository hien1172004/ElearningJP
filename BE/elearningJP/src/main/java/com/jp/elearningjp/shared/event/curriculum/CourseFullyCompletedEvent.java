package com.jp.elearningjp.shared.event.curriculum;

import lombok.Getter;

/**
 * Event phát ra khi user hoàn thành 100% lessons trong một course
 * (Enrollment status chuyển sang COMPLETED).
 */
@Getter
public class CourseFullyCompletedEvent extends CurriculumEvent {

    private final Long userId;
    private final Long courseId;
    private final Long enrollmentId;

    public CourseFullyCompletedEvent(Object source, Long userId, Long courseId, Long enrollmentId) {
        super(source);
        this.userId = userId;
        this.courseId = courseId;
        this.enrollmentId = enrollmentId;
    }
}