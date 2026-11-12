package com.jp.elearningjp.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AppException extends RuntimeException {
    private ErrorCode errorCode;
    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * Giữ mã lỗi nhưng thay message bằng nội dung cụ thể hơn (vd: "đã được Nguyễn A duyệt lúc 10:32").
     * {@code detailMessage} null/rỗng → dùng message mặc định của mã lỗi.
     */
    public AppException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage == null || detailMessage.isBlank() ? errorCode.getMessage() : detailMessage);
        this.errorCode = errorCode;
    }
}