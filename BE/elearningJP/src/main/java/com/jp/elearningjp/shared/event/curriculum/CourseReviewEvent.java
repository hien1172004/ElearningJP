package com.jp.elearningjp.shared.event.curriculum;

import lombok.Getter;

/**
 * Sự kiện trong luồng duyệt khóa học — listener gửi thông báo sau khi giao dịch commit:
 * SUBMITTED / WITHDRAWN → mọi ADMIN; APPROVED / SAVED / REJECTED / PUBLISHED / UNPUBLISHED → giáo viên được giao.
 */
@Getter
public class CourseReviewEvent extends CurriculumEvent {

    /** SAVED: ADMIN tự chốt phiên bản mới từ bản đang soạn (không qua duyệt). */
    public enum Action { SUBMITTED, WITHDRAWN, APPROVED, SAVED, REJECTED, PUBLISHED, UNPUBLISHED }

    private final Long courseId;
    private final String courseTitle;
    private final Action action;
    /** Người thao tác — không gửi thông báo cho chính họ. */
    private final Long actorId;
    /** Ghi chú khi gửi / lý do từ chối (có thể null). */
    private final String note;
    /** APPROVED / SAVED khi khóa đang xuất bản → học viên nhận "khóa có nội dung mới". */
    private final boolean contentUpdatedWhilePublished;

    public CourseReviewEvent(Object source, Long courseId, String courseTitle, Action action,
                             Long actorId, String note, boolean contentUpdatedWhilePublished) {
        super(source);
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.action = action;
        this.actorId = actorId;
        this.note = note;
        this.contentUpdatedWhilePublished = contentUpdatedWhilePublished;
    }
}
