package com.jp.elearningjp.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    UNCAUGHT_EXCEPTION(9999, "Uncaught exception", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_EXISTED(1001, "User already existed", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1002, "User not found", HttpStatus.NOT_FOUND),
    USERNAME_EXISTED(1003, "Username already existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1004, "Username is invalid", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1005, "Password is invalid", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(1006, "Wrong password", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1007, "Email already existed", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1008, "Email is invalid", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1009, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    ROLE_INVALID(1010, "Role is invalid", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1011, "U dont have permission", HttpStatus.FORBIDDEN),
    PASSWORD_CONFIRM_NOT_MATCH(1012, "Password confirm not match", HttpStatus.BAD_REQUEST),
    USER_BANNED(1013, "User is banned", HttpStatus.FORBIDDEN),
    INVALID_RESET_TOKEN(1014, "Reset token is invalid", HttpStatus.BAD_REQUEST),
    SAME_OLD_PASSWORD(1015, "New password must be different from old password", HttpStatus.BAD_REQUEST),

    // Curriculum & Quiz (2000 - 2999)
    COURSE_NOT_FOUND(2001, "Không tìm thấy khóa học", HttpStatus.NOT_FOUND),
    COURSE_NOT_ENROLLED(2002, "Bạn chưa đăng ký khóa học này. Vui lòng đăng ký để học bài và làm bài kiểm tra!", HttpStatus.FORBIDDEN),
    ALREADY_ENROLLED(2003, "Bạn đã đăng ký khóa học này rồi", HttpStatus.BAD_REQUEST),
    LESSON_NOT_FOUND(2004, "Không tìm thấy bài học", HttpStatus.NOT_FOUND),
    QUIZ_NOT_FOUND(2005, "Bài học này chưa có câu hỏi kiểm tra", HttpStatus.NOT_FOUND),
    QUESTION_NOT_FOUND(2006, "Không tìm thấy câu hỏi", HttpStatus.NOT_FOUND),
    SLUG_EXISTED(2007, "Đường dẫn khóa học (slug) đã tồn tại", HttpStatus.BAD_REQUEST),

    // SRS Module (3000 - 3999)
    SRS_ITEM_NOT_FOUND(3001, "Không tìm thấy thẻ ôn tập", HttpStatus.NOT_FOUND),
    SRS_ITEM_ALREADY_EXISTS(3002, "Mục này đã tồn tại trong danh sách ôn tập SRS của bạn", HttpStatus.BAD_REQUEST),
    SRS_ITEM_ACCESS_DENIED(3003, "Bạn không có quyền truy cập thẻ ôn tập này", HttpStatus.FORBIDDEN),
    VOCAB_NOT_FOUND(3004, "Không tìm thấy từ vựng tương ứng", HttpStatus.NOT_FOUND),
    CHARACTER_NOT_FOUND(3005, "Không tìm thấy chữ Hán tương ứng", HttpStatus.NOT_FOUND),
    GRAMMAR_NOT_FOUND(3006, "Không tìm thấy điểm ngữ pháp tương ứng", HttpStatus.NOT_FOUND),
    INVALID_SRS_REQUEST(3007, "Yêu cầu thêm vào SRS không hợp lệ (cần chọn từ vựng, chữ Hán hoặc ngữ pháp)", HttpStatus.BAD_REQUEST)
    ;


     int code;
     String message;
     HttpStatusCode httpStatusCode;

}