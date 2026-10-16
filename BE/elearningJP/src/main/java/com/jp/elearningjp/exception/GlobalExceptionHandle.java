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
        apiResponse.setMessage(errorCode.getMessage());
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
        String enumKey = e.getFieldError() != null ? e.getFieldError().getDefaultMessage() : "Validation error";
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
                        .message("Invalid JSON request body format")
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
     *   <li>Các constraint khác → fallback 409 với message generic</li>
     * </ul>
     */
    @ExceptionHandler(value = org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException e) {
        String rootMsg = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : e.getMessage();
        boolean isOrderIndexConflict = rootMsg != null
                && rootMsg.toLowerCase().contains("order_index");

        ErrorCode errorCode = isOrderIndexConflict
                ? ErrorCode.LESSON_ORDER_CONFLICT
                : ErrorCode.UNCAUGHT_EXCEPTION;

        if (isOrderIndexConflict) {
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandle.class)
                    .warn("[DB-CONFLICT] Order index unique constraint violated: {}", rootMsg);
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