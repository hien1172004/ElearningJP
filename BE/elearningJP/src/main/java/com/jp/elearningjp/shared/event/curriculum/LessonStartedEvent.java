package com.jp.elearningjp.shared.event.curriculum;

import lombok.Getter;

/**
 * Event phát ra khi user start một lesson (chuyển sang IN_PROGRESS).
 * Consumer tương lai: analytics, adaptive learning.
 */
@Getter
public class LessonStartedEvent extends CurriculumEvent {

    private final Long userId;
    private final Long lessonId;
    private final Long courseId;

    public LessonStartedEvent(Object source, Long userId, Long lessonId, Long courseId) {
        super(source);
        this.userId = userId;
        this.lessonId = lessonId;
        this.courseId = courseId;
    }
}