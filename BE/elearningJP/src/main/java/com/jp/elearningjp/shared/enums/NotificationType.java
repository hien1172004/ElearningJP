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
    COURSE_REVIEW,
    /** Đề thi được cập nhật phiên bản mới hoặc đính chính nội dung. */
    EXAM_UPDATED,
    /** Duyệt theo bài: giáo viên gửi bài lên duyệt (gửi ADMIN, 1 thông báo cho cả lượt). */
    LESSON_SUBMITTED,
    /** Duyệt theo bài: giáo viên rút yêu cầu duyệt bài (gửi ADMIN). */
    LESSON_WITHDRAWN,
    /** Duyệt theo bài: ADMIN duyệt bài (gửi giáo viên đã gửi). */
    LESSON_APPROVED,
    /** Duyệt theo bài: ADMIN từ chối bài kèm lý do (gửi giáo viên đã gửi). */
    LESSON_REJECTED,
    /** Bản học viên đang học của bài vừa đổi → người còn bản chưa lưu (autosave) của bài đó: bản đó đã cũ. */
    DRAFT_STALE,
    /** Bài vừa có thay đổi lớn (ADMIN đánh dấu) → học viên đã hoàn thành bài. */
    LESSON_UPDATED,
    /** ADMIN bỏ thay đổi chưa duyệt của bài (bản nháp quay về bản live) → giáo viên được giao. */
    LESSON_DRAFT_DISCARDED
}
