package com.jp.elearningjp.dto.response.curriculum.importer;

/** Trạng thái 1 dòng Excel sau khi kiểm tra. */
public enum ImportRowStatus {
    /** Hợp lệ, sẽ được import. */
    OK,
    /** Hợp lệ nhưng có lưu ý (vd: từ có nhiều nghĩa, đã lấy nghĩa đầu). */
    WARNING,
    /** Không hợp lệ — cả file không được import cho tới khi sửa. */
    ERROR
}
