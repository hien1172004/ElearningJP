package com.jp.elearningjp.shared.event.curriculum;

import lombok.Getter;

/**
 * ADMIN vừa giao hoặc gỡ một giáo viên khỏi khóa học.
 * Listener gửi thông báo cho giáo viên đó sau khi giao dịch commit.
 */
@Getter
public class CourseTeacherAssignmentChangedEvent extends CurriculumEvent {

    private final Long courseId;
    private final String courseTitle;
    private final Long teacherId;
    /** true = vừa được giao, false = vừa bị gỡ */
    private final boolean assigned;
    /** ADMIN thao tác */
    private final Long changedByUserId;

    public CourseTeacherAssignmentChangedEvent(Object source, Long courseId, String courseTitle,
                                               Long teacherId, boolean assigned, Long changedByUserId) {
        super(source);
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.teacherId = teacherId;
        this.assigned = assigned;
        this.changedByUserId = changedByUserId;
    }
}
