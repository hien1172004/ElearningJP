package com.jp.elearningjp.shared.enums;

public enum NotificationType {
    SRS_DUE,
    STREAK_WARNING,
    TASK_REMINDER,
    EXAM_RESULT,
    BADGE_UNLOCKED,
    COMMUNITY_REPLY,
    COMMUNITY_LIKE,
    COMMUNITY_DISLIKE,
    /** Bình luận / trả lời của người dùng bị người kiểm duyệt ẩn hoặc xóa. */
    COMMENT_MODERATED,
    /** Thông báo hàng loạt do ADMIN / TEACHER gửi (notification_broadcasts). */
    ANNOUNCEMENT,
    /** Khóa học đã đăng ký được chuyển giữa bản nháp và xuất bản. */
    COURSE_STATUS_CHANGED,
    /** Giáo viên được ADMIN giao hoặc gỡ khỏi một khóa học. */
    COURSE_TEACHER_ASSIGNMENT,
    /** Luồng duyệt khóa học: gửi / rút / duyệt / từ chối / xuất bản / chuyển nháp (data_json.action). */
    COURSE_REVIEW
}
