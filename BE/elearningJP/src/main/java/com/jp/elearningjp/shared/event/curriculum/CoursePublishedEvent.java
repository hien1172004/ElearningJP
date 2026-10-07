package com.jp.elearningjp.shared.event.curriculum;

import lombok.Getter;

/**
 * Event phát ra khi Course chuyển trạng thái {@code published == true}.
 * Consumer tương lai có thể: gửi thông báo cho user theo dõi JLPT level,
 * đồng bộ search index, cập nhật recommendation engine, ...
 */
@Getter
public class CoursePublishedEvent extends CurriculumEvent {

    private final Long courseId;
    private final String title;
    private final String jlptLevel;
    private final Long publishedByUserId;

    public CoursePublishedEvent(Object source, Long courseId, String title, String jlptLevel, Long publishedByUserId) {
        super(source);
        this.courseId = courseId;
        this.title = title;
        this.jlptLevel = jlptLevel;
        this.publishedByUserId = publishedByUserId;
    }
}