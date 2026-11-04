package com.jp.elearningjp.shared.enums;

/**
 * Trạng thái vòng đời thẻ SRS.
 * <pre>
 * NEW → LEARNING → REVIEWING (interval ≥ 6) → MASTERED (interval ≥ 21)
 *                      └── AGAIN ──► RELEARNING ──► REVIEWING
 * Bất kỳ trạng thái → ARCHIVED (quá hạn > N ngày / người dùng xoá) → NEW (kích hoạt lại)
 * </pre>
 */
public enum SrsStatus {
    /** Mới thêm, chưa ôn lần nào. Bị giới hạn số thẻ mới mỗi ngày. */
    NEW,
    /** Đang học lần đầu (chưa đạt interval 6 ngày). */
    LEARNING,
    /** Đã nhớ, đang giãn cách ôn tập. */
    REVIEWING,
    /** Đã từng nhớ nhưng bị quên (AGAIN), đang học lại. */
    RELEARNING,
    /** Nhớ lâu dài (interval ≥ 21 ngày). */
    MASTERED,
    /** Đã lưu trữ — ẩn khỏi hàng ôn/thống kê, giữ lịch sử, có thể kích hoạt lại. */
    ARCHIVED
}
