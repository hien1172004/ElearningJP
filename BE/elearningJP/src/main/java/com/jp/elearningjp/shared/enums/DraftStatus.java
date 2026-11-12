package com.jp.elearningjp.shared.enums;

/** Trạng thái bản nháp của bài học (cột {@code lesson_drafts.status}, V65). */
public enum DraftStatus {
    /** Đang soạn — người giữ khóa sửa khóa học của phía này ghi được */
    EDITING,
    /** Giáo viên đã gửi duyệt — chỉ đọc tới khi ADMIN duyệt / từ chối hoặc giáo viên rút yêu cầu */
    SUBMITTED,
    /** Nháp cũ chỉ đọc, giữ lại khi "cập nhật bản mới" để người dùng tự chép phần còn thiếu */
    ARCHIVED
}
