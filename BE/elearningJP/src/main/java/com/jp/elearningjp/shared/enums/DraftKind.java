package com.jp.elearningjp.shared.enums;

/**
 * Phía sở hữu bản nháp của bài học (cột {@code lesson_drafts.kind}, V65).
 * Mỗi bài có đúng 1 nháp mỗi phía; hai phía không thấy nháp của nhau.
 */
public enum DraftKind {
    /** Nháp chung của các giáo viên được giao khóa */
    TEACHER,
    /** Nháp chung của các quản trị viên */
    ADMIN
}
