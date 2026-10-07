package com.jp.elearningjp.shared.event.curriculum;

import lombok.Getter;

/**
 * Event phát ra khi Course được cập nhật (title/description/thumbnail/...).
 */
@Getter
public class CourseUpdatedEvent extends CurriculumEvent {

    private final Long courseId;
    private final Long updatedByUserId;

    public CourseUpdatedEvent(Object source, Long courseId, Long updatedByUserId) {
        super(source);
        this.courseId = courseId;
        this.updatedByUserId = updatedByUserId;
    }
}