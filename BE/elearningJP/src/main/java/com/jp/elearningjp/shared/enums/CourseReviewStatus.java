package com.jp.elearningjp.shared.enums;

/**
 * Trạng thái bản đang soạn của khóa học (cột {@code courses.review_status}).
 * Giá trị khớp ràng buộc CHECK trong database.
 */
public enum CourseReviewStatus {
    /** Đang soạn — giáo viên được giao sửa được nội dung */
    EDITING,
    /** Đã gửi duyệt — giáo viên bị khóa sửa tới khi ADMIN quyết định hoặc yêu cầu bị rút */
    PENDING_REVIEW
}
