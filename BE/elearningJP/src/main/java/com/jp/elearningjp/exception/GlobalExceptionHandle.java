package com.jp.elearningjp.exception;

import com.jp.elearningjp.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandle {

    // @ExceptionHandler(value = Exception.class)
    // public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
    // ApiResponse<?> apiResponse = new ApiResponse<>();
    // ErrorCode errorCode = ErrorCode.UNCAUGHT_EXCEPTION;
    // apiResponse.setCode(errorCode.getCode());
    // apiResponse.setMessage(errorCode.getMessage());
    // return
    // ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    // }

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<?> handleAppException(AppException e) {
        ErrorCode errorCode = e.getErrorCode();
        ApiResponse<?> apiResponse = new ApiResponse<>();
        apiResponse.setCode(errorCode.getCode());
        // Mặc định = message của mã lỗi; AppException(code, detail) cho message cụ thể hơn
        apiResponse.setMessage(e.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException e) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(
                ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String enumKey = e.getFieldError() != null ? e.getFieldError().getDefaultMessage() : "Dữ liệu không hợp lệ";
        ErrorCode errorCode;
        try {
            errorCode = ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .code(400)
                            .message(enumKey)
                            .build());
        }
        ApiResponse<Object> apiResponse = new ApiResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .code(400)
                        .message("Dữ liệu gửi lên không đúng định dạng")
                        .build());
    }

    /**
     * Bắt lỗi vi phạm UNIQUE constraint từ DB (ví dụ: trùng order_index
     * do race condition giữa EXISTS check và INSERT/UPDATE).
     *
     * <p>Mapping:</p>
     * <ul>
     *   <li>Postgres error code 23505 (unique_violation) liên quan đến
     *       {@code order_index} → {@link ErrorCode#LESSON_ORDER_CONFLICT} (409)</li>
     *   <li>{@code uq_course_teachers_course_teacher} → {@link ErrorCode#COURSE_TEACHER_ALREADY_ASSIGNED} (409)</li>
     *   <li>{@code uq_course_review_requests_pending} → {@link ErrorCode#COURSE_REVIEW_ALREADY_PENDING} (409)</li>
     *   <li>Các constraint khác → {@link ErrorCode#UNCAUGHT_EXCEPTION} (500)</li>
     * </ul>
     */
    @ExceptionHandler(value = org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException e) {
        String rootMsg = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : e.getMessage();
        ErrorCode known = knownConstraintError(rootMsg);
        ErrorCode errorCode = known != null ? known : ErrorCode.UNCAUGHT_EXCEPTION;

        if (known != null) {
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandle.class)
                    .warn("[DB-CONFLICT] Unique constraint violated ({}): {}", known, rootMsg);
        } else {
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandle.class)
                    .error("[DB-CONFLICT] Data integrity violation: ", e);
        }

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(
                ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    /**
     * Ràng buộc DB bị vi phạm khi 2 thao tác chạy song song cùng vượt qua bước kiểm tra ở service
     * → trả mã nghiệp vụ 409 như khi bị chặn tuần tự, không trả 500. Không nhận ra → null.
     */
    static ErrorCode knownConstraintError(String rootMsg) {
        if (rootMsg == null) {
            return null;
        }
        String msg = rootMsg.toLowerCase(java.util.Locale.ROOT);
        if (msg.contains("uq_course_teachers_course_teacher")) {
            return ErrorCode.COURSE_TEACHER_ALREADY_ASSIGNED;
        }
        if (msg.contains("uq_course_review_requests_pending")) {
            return ErrorCode.COURSE_REVIEW_ALREADY_PENDING;
        }
        if (msg.contains("uq_lesson_review_requests_pending")) {
            return ErrorCode.LESSON_REVIEW_ALREADY_PENDING;
        }
        if (msg.contains("order_index")) {
            return ErrorCode.LESSON_ORDER_CONFLICT;
        }
        return null;
    }

    /**
     * Hai giao dịch cùng sửa 1 bản ghi chồng nhau (JPA {@code @Version}) → 409 thay vì 500,
     * để FE báo người dùng tải lại.
     */
    @ExceptionHandler(value = {
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class
    })
    public ResponseEntity<ApiResponse<?>> handleOptimisticLock(Exception e) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandle.class)
                .warn("[CONCURRENT] Optimistic lock conflict: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.CONCURRENT_MODIFICATION;
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(
                ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(Exception e) {
        // ClientAbort / AsyncRequestNotUsable = client closed connection before response was sent.
        // This is not a real error — log at DEBUG and return 499 (no body).
        if (e instanceof org.springframework.web.context.request.async.AsyncRequestNotUsableException
                || e instanceof org.apache.catalina.connector.ClientAbortException
                || (e.getCause() instanceof java.io.IOException
                        && e.getCause().getMessage() != null
                        && e.getCause().getMessage().toLowerCase().contains("connection reset"))) {
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandle.class)
                    .debug("Client closed connection (ignored): {}", e.getMessage());
            return null; // 499
        }
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandle.class).error("Uncaught exception: ", e);
        ErrorCode errorCode = ErrorCode.UNCAUGHT_EXCEPTION;
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(
                ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }
}