package com.jp.elearningjp.shared.event.curriculum;

import lombok.Getter;

/**
 * Event phát ra khi user enroll vào một course.
 * Consumer tương lai có thể: khởi tạo LearningPath, cập nhật daily task quota,
 * gửi email "Welcome to course X", analytics, ...
 */
@Getter
public class UserEnrolledEvent extends CurriculumEvent {

    private final Long userId;
    private final Long courseId;
    private final Long enrollmentId;

    public UserEnrolledEvent(Object source, Long userId, Long courseId, Long enrollmentId) {
        super(source);
        this.userId = userId;
        this.courseId = courseId;
        this.enrollmentId = enrollmentId;
    }
}