package com.jp.elearningjp.shared.enums;

/** Đối tượng nhận thông báo hàng loạt — khớp CHECK của notification_broadcasts.audience. */
public enum BroadcastAudience {
    /** Mọi tài khoản đang hoạt động (chỉ ADMIN). */
    ALL_USERS,
    /** Tài khoản có role chỉ định (chỉ ADMIN). */
    ROLE,
    /** Học viên đã đăng ký một khóa học (ADMIN: mọi khóa; TEACHER: khóa của mình). */
    COURSE_STUDENTS
}
