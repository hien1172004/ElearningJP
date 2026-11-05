package com.jp.elearningjp.shared.event.curriculum;

import lombok.Getter;

/**
 * Event phát ra khi khóa học được chuyển giữa bản nháp và xuất bản (chỉ khi trạng thái thật sự đổi).
 * Consumer: gửi thông báo tới học viên đã đăng ký khóa học.
 */
@Getter
public class CoursePublishStatusChangedEvent extends CurriculumEvent {

    private final Long courseId;
    private final String courseTitle;
    /** Trạng thái MỚI: true = vừa xuất bản, false = vừa chuyển về bản nháp */
    private final boolean published;
    /** Người thao tác (giáo viên / admin) — không gửi thông báo cho chính họ */
    private final Long changedByUserId;

    public CoursePublishStatusChangedEvent(Object source, Long courseId, String courseTitle,
                                           boolean published, Long changedByUserId) {
        super(source);
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.published = published;
        this.changedByUserId = changedByUserId;
    }
}
