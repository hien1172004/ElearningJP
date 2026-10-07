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

// CURRICULUM MODULE — Course (1100-1199)
    COURSE_NOT_FOUND(1100, "Course not found", HttpStatus.NOT_FOUND),
    COURSE_NOT_PUBLISHED(1101, "Course is not published", HttpStatus.BAD_REQUEST),
    COURSE_ARCHIVED(1102, "Course is archived", HttpStatus.BAD_REQUEST),
    COURSE_SLUG_ALREADY_EXISTS(1103, "Course slug already exists", HttpStatus.CONFLICT),
    COURSE_DELETED(1104, "Course has been deleted", HttpStatus.GONE),

    // CURRICULUM MODULE — Lesson (1200-1299)
    LESSON_NOT_FOUND(1200, "Lesson not found", HttpStatus.NOT_FOUND),
    LESSON_TYPE_INVALID(1201, "Invalid lesson type", HttpStatus.BAD_REQUEST),
    LESSON_ORDER_CONFLICT(1202, "Order index already used in this course", HttpStatus.CONFLICT),

    // CURRICULUM MODULE — Enrollment (1300-1399)
    ENROLLMENT_NOT_FOUND(1300, "Enrollment not found", HttpStatus.NOT_FOUND),
    ALREADY_ENROLLED(1301, "User already enrolled in this course", HttpStatus.CONFLICT),
    NOT_ENROLLED(1302, "User is not enrolled in this course", HttpStatus.FORBIDDEN),
    ENROLLMENT_DROPPED(1303, "Enrollment has been dropped", HttpStatus.BAD_REQUEST),

    // CURRICULUM MODULE — Progress (1400-1499)
    PROGRESS_NOT_FOUND(1400, "Lesson progress not found", HttpStatus.NOT_FOUND),
    PROGRESS_ALREADY_COMPLETED(1401, "Lesson already completed", HttpStatus.BAD_REQUEST),
    CANNOT_COMPLETE_LESSON(1402, "Cannot mark this lesson as complete", HttpStatus.BAD_REQUEST),
    INVALID_BEST_SCORE(1403, "Best score is invalid for non MINI_TEST course", HttpStatus.BAD_REQUEST),

    // CURRICULUM MODULE — Authorization (1500-1599)
    FORBIDDEN_COURSE_ACCESS(1500, "You don't have permission to access this course", HttpStatus.FORBIDDEN),
    FORBIDDEN_LESSON_ACCESS(1501, "You don't have permission to access this lesson", HttpStatus.FORBIDDEN),
    FORBIDDEN_PROGRESS_ACCESS(1502, "You don't have permission to update this progress", HttpStatus.FORBIDDEN),

    // CURRICULUM MODULE — Quiz (2000-2999)
    COURSE_NOT_ENROLLED(2002, "You are not enrolled in this course. Please enroll to access lessons and quizzes!", HttpStatus.FORBIDDEN),
    QUIZ_NOT_FOUND(2005, "This lesson has no quiz", HttpStatus.NOT_FOUND),
    QUESTION_NOT_FOUND(2006, "Question not found", HttpStatus.NOT_FOUND),

    // SRS Module (3000 - 3999)
    SRS_ITEM_NOT_FOUND(3001, "Srs item not found", HttpStatus.NOT_FOUND),
    SRS_ITEM_ALREADY_EXISTS(3002, "Item already exists in your SRS review list", HttpStatus.BAD_REQUEST),
    SRS_ITEM_ACCESS_DENIED(3003, "You don't have permission to access this Srs item", HttpStatus.FORBIDDEN),
    VOCAB_NOT_FOUND(3004, "Corresponding vocab not found", HttpStatus.NOT_FOUND),
    CHARACTER_NOT_FOUND(3005, "Corresponding kanji not found", HttpStatus.NOT_FOUND),
    GRAMMAR_NOT_FOUND(3006, "Corresponding grammar point not found", HttpStatus.NOT_FOUND),
    INVALID_SRS_REQUEST(3007, "Invalid SRS add request (need to select vocab, kanji or grammar)", HttpStatus.BAD_REQUEST)
    ;


     int code;
     String message;
     HttpStatusCode httpStatusCode;

}