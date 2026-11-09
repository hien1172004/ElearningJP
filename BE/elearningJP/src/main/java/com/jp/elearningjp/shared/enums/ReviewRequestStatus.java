package com.jp.elearningjp.shared.enums;

/**
 * Trạng thái một lượt gửi duyệt khóa học (cột {@code course_review_requests.status}).
 * Giá trị khớp ràng buộc CHECK trong database.
 */
public enum ReviewRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    WITHDRAWN
}
