package com.jp.elearningjp.shared.event.curriculum;

import lombok.Getter;

/**
 * Event phát ra khi một Course được tạo mới.
 */
@Getter
public class CourseCreatedEvent extends CurriculumEvent {

    private final Long courseId;
    private final String title;
    private final Long createdByUserId;

    public CourseCreatedEvent(Object source, Long courseId, String title, Long createdByUserId) {
        super(source);
        this.courseId = courseId;
        this.title = title;
        this.createdByUserId = createdByUserId;
    }
}