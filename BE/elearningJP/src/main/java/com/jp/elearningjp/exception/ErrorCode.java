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
    CONCURRENT_MODIFICATION(9001, "Nội dung đã bị người khác thay đổi, vui lòng tải lại trước khi lưu", HttpStatus.CONFLICT),
    UNCAUGHT_EXCEPTION(9999, "Đã xảy ra lỗi không mong muốn, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_EXISTED(1001, "Email này đã được sử dụng", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1002, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    USERNAME_EXISTED(1003, "Tên đăng nhập đã tồn tại", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1004, "Tên đăng nhập không hợp lệ", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1005, "Mật khẩu không đúng", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(1006, "Sai mật khẩu", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1007, "Email đã tồn tại", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1008, "Email không hợp lệ", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1009, "Bạn cần đăng nhập để tiếp tục", HttpStatus.UNAUTHORIZED),
    ROLE_INVALID(1010, "Vai trò không hợp lệ", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1011, "Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    PASSWORD_CONFIRM_NOT_MATCH(1012, "Mật khẩu nhập lại không khớp", HttpStatus.BAD_REQUEST),
    USER_BANNED(1013, "Tài khoản đã bị khóa", HttpStatus.FORBIDDEN),
    INVALID_RESET_TOKEN(1014, "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    SAME_OLD_PASSWORD(1015, "Mật khẩu mới phải khác mật khẩu cũ", HttpStatus.BAD_REQUEST),
    CANNOT_MODIFY_OWN_ACCOUNT(1016, "Bạn không thể thay đổi trạng thái hoặc vai trò của chính mình", HttpStatus.BAD_REQUEST),
    PREMIUM_REQUIRED(1050, "Nội dung này yêu cầu gói hội viên PRO", HttpStatus.PAYMENT_REQUIRED),
    TOKEN_EXHAUSTED(1051, "Bạn đã hết lượt token AI hôm nay", HttpStatus.PAYMENT_REQUIRED),
    ORDER_NOT_FOUND(1052, "Không tìm thấy đơn hàng", HttpStatus.NOT_FOUND),

// CURRICULUM MODULE — Course (1100-1199)
    COURSE_NOT_FOUND(1100, "Không tìm thấy khóa học", HttpStatus.NOT_FOUND),
    COURSE_NOT_PUBLISHED(1101, "Khóa học chưa được xuất bản", HttpStatus.BAD_REQUEST),
    COURSE_ARCHIVED(1102, "Khóa học đã được lưu trữ", HttpStatus.BAD_REQUEST),
    COURSE_SLUG_ALREADY_EXISTS(1103, "Đường dẫn (slug) của khóa học đã tồn tại", HttpStatus.CONFLICT),
    COURSE_DELETED(1104, "Khóa học đã bị xóa", HttpStatus.GONE),
    COURSE_TEACHER_INVALID(1105, "Người dùng này không phải giáo viên đang hoạt động", HttpStatus.BAD_REQUEST),
    COURSE_TEACHER_ALREADY_ASSIGNED(1106, "Giáo viên này đã được giao khóa học", HttpStatus.CONFLICT),
    COURSE_TEACHER_NOT_ASSIGNED(1107, "Giáo viên này chưa được giao khóa học", HttpStatus.NOT_FOUND),
    COURSE_PENDING_REVIEW_LOCKED(1108, "Khóa học đang chờ duyệt nên tạm khóa chỉnh sửa. Hãy rút yêu cầu duyệt nếu cần sửa tiếp", HttpStatus.CONFLICT),
    COURSE_REVIEW_ALREADY_PENDING(1109, "Khóa học đã có yêu cầu duyệt đang chờ", HttpStatus.CONFLICT),
    COURSE_REVIEW_NOT_PENDING(1110, "Yêu cầu duyệt không còn ở trạng thái chờ", HttpStatus.CONFLICT),
    COURSE_REVIEW_NO_CHANGES(1111, "Khóa học không có thay đổi nào mới để gửi duyệt — nội dung hiện tại đã được chốt (có thể quản trị viên vừa lưu). Hãy tải lại trang", HttpStatus.BAD_REQUEST),
    COURSE_REVIEW_NO_LESSONS(1112, "Khóa học cần có ít nhất 1 bài học trước khi gửi duyệt", HttpStatus.BAD_REQUEST),
    COURSE_REVIEW_REQUEST_NOT_FOUND(1113, "Không tìm thấy yêu cầu duyệt", HttpStatus.NOT_FOUND),
    COURSE_REVIEW_WITHDRAW_FORBIDDEN(1114, "Chỉ giáo viên đang được giao khóa học mới rút được yêu cầu duyệt", HttpStatus.FORBIDDEN),
    COURSE_NOT_YET_APPROVED(1115, "Khóa học chưa có bản nào được duyệt — cần duyệt yêu cầu của giáo viên hoặc lưu phiên bản trước", HttpStatus.BAD_REQUEST),
    COURSE_REVIEW_COMMENT_NOT_FOUND(1117, "Không tìm thấy nhận xét duyệt", HttpStatus.NOT_FOUND),
    COURSE_REVIEW_COMMENT_TARGET_INVALID(1118, "Bài học hoặc mục được nhận xét không thuộc khóa học này", HttpStatus.BAD_REQUEST),
    COURSE_REVIEW_SUBMIT_FORBIDDEN(1119, "Chỉ giáo viên được giao mới gửi khóa học lên duyệt", HttpStatus.FORBIDDEN),
    COURSE_SAVE_PENDING_REVIEW(1120, "Khóa học đang có yêu cầu duyệt chờ xử lý — hãy duyệt hoặc từ chối yêu cầu đó", HttpStatus.CONFLICT),
    COURSE_SAVE_NO_CHANGES(1121, "Bản đang soạn không có thay đổi nào để lưu", HttpStatus.BAD_REQUEST),
    COURSE_SAVE_NO_LESSONS(1122, "Khóa học cần có ít nhất 1 bài học trước khi lưu phiên bản", HttpStatus.BAD_REQUEST),
    COURSE_REVIEW_LESSONS_PENDING(1123, "Khóa học đang có bài học chờ duyệt — hãy duyệt / từ chối các bài đó trước", HttpStatus.CONFLICT),

    // CURRICULUM MODULE — Lesson (1200-1299)
    LESSON_NOT_FOUND(1200, "Không tìm thấy bài học", HttpStatus.NOT_FOUND),
    LESSON_TYPE_INVALID(1201, "Loại bài học không hợp lệ", HttpStatus.BAD_REQUEST),
    LESSON_ORDER_CONFLICT(1202, "Thứ tự này đã được dùng cho một bài học khác trong khóa", HttpStatus.CONFLICT),
    LESSON_SECTION_NOT_FOUND(1203, "Không tìm thấy mục của bài học", HttpStatus.NOT_FOUND),
    LESSON_SECTION_TYPE_INVALID(1204, "Loại mục không hợp lệ", HttpStatus.BAD_REQUEST),
    LESSON_KANJI_ITEM_NOT_FOUND(1205, "Không tìm thấy chữ Kanji trong bài học", HttpStatus.NOT_FOUND),
    LESSON_VOCABULARY_ITEM_NOT_FOUND(1206, "Không tìm thấy từ vựng trong bài học", HttpStatus.NOT_FOUND),
    LESSON_GRAMMAR_ITEM_NOT_FOUND(1207, "Không tìm thấy ngữ pháp trong bài học", HttpStatus.NOT_FOUND),
    LESSON_KANJI_ITEM_DUPLICATED(1208, "Kanji đã tồn tại trong section này", HttpStatus.CONFLICT),
    LESSON_VOCABULARY_ITEM_DUPLICATED(1209, "Vocabulary đã tồn tại trong section này", HttpStatus.CONFLICT),
    LESSON_GRAMMAR_ITEM_DUPLICATED(1210, "Grammar đã tồn tại trong section này", HttpStatus.CONFLICT),
    LESSON_KANJI_RELATED_VOCAB_DUPLICATED(1211, "Related vocabulary đã tồn tại trong kanji item này", HttpStatus.CONFLICT),
    LESSON_KANJI_RELATED_VOCAB_NOT_FOUND(1212, "Related vocabulary không tồn tại", HttpStatus.NOT_FOUND),
    LESSON_VOCAB_EXAMPLE_DUPLICATED(1213, "Ví dụ trùng lặp trong cùng request", HttpStatus.CONFLICT),
    LESSON_VOCAB_EXAMPLE_MULTIPLE_PRIMARY(1214, "Chỉ được phép có tối đa 1 ví dụ primary", HttpStatus.BAD_REQUEST),
    LESSON_VOCAB_EXAMPLE_NOT_FOUND(1215, "Ví dụ từ vựng không tồn tại", HttpStatus.NOT_FOUND),
    LESSON_GRAMMAR_EXAMPLE_DUPLICATED(1216, "Ví dụ trùng lặp trong cùng request", HttpStatus.CONFLICT),
    LESSON_GRAMMAR_EXAMPLE_NOT_FOUND(1217, "Ví dụ ngữ pháp không tồn tại", HttpStatus.NOT_FOUND),
    LESSON_REORDER_LIST_EMPTY(1218, "Danh sách lesson cần reorder không được rỗng", HttpStatus.BAD_REQUEST),
    LESSON_REORDER_LIST_INVALID(1219, "Danh sách lesson cần reorder chứa ID trùng lặp hoặc không hợp lệ", HttpStatus.BAD_REQUEST),
    LESSON_REORDER_LIST_MISMATCH(1220, "Danh sách lesson cần reorder phải khớp 100% với tất cả lesson hiện có trong khóa học", HttpStatus.CONFLICT),
    // Import item section từ Excel (1230-1239)
    IMPORT_FILE_EMPTY(1230, "Chưa chọn file Excel hoặc file rỗng", HttpStatus.BAD_REQUEST),
    IMPORT_FILE_INVALID(1231, "File không đúng định dạng .xlsx hoặc bị hỏng", HttpStatus.BAD_REQUEST),
    IMPORT_FILE_TOO_LARGE(1232, "File Excel vượt quá 2MB", HttpStatus.BAD_REQUEST),
    IMPORT_TEMPLATE_INVALID(1233, "File không đúng mẫu Excel của hệ thống, hãy tải mẫu mới nhất", HttpStatus.BAD_REQUEST),
    IMPORT_TEMPLATE_TYPE_MISMATCH(1234, "Mẫu Excel không khớp loại section đang import", HttpStatus.BAD_REQUEST),
    IMPORT_TOO_MANY_ROWS(1235, "File Excel vượt quá số dòng cho phép", HttpStatus.BAD_REQUEST),
    IMPORT_NO_ROWS(1236, "File Excel không có dòng dữ liệu nào", HttpStatus.BAD_REQUEST),
    IMPORT_HAS_ERRORS(1237, "File Excel còn dòng lỗi, hãy sửa rồi import lại", HttpStatus.BAD_REQUEST),
    // In phiếu luyện viết từ bài học (1238-1239, 1222)
    PRACTICE_SHEET_NO_ITEMS(1238, "Chưa chọn từ nào để in phiếu luyện viết", HttpStatus.BAD_REQUEST),
    PRACTICE_SHEET_TYPE_INVALID(1239, "Chỉ in phiếu luyện viết cho mục Chữ Hán hoặc Từ vựng", HttpStatus.BAD_REQUEST),
    PRACTICE_SHEET_TOO_MANY_ITEMS(1222, "Mỗi lần in tối đa 300 mục, hãy chọn bớt", HttpStatus.BAD_REQUEST),
    LESSON_KANJI_RELATED_VOCAB_TOO_MANY(1221, "Số từ vựng liên quan vượt quá giới hạn 200 từ cho 1 chữ Hán", HttpStatus.BAD_REQUEST),

    // Duyệt theo bài (V62)
    LESSON_PENDING_REVIEW_LOCKED(1240, "Bài học đang chờ duyệt nên tạm khóa chỉnh sửa. Cần quản trị viên duyệt / từ chối, hoặc giáo viên rút yêu cầu", HttpStatus.CONFLICT),
    LESSON_REVIEW_ALREADY_PENDING(1241, "Bài học đã có yêu cầu duyệt đang chờ", HttpStatus.CONFLICT),
    LESSON_REVIEW_NOT_PENDING(1242, "Yêu cầu duyệt bài học không còn ở trạng thái chờ", HttpStatus.CONFLICT),
    LESSON_REVIEW_NO_CHANGES(1243, "Bài học không có thay đổi nào mới so với bản đang học", HttpStatus.BAD_REQUEST),
    LESSON_REVIEW_REQUEST_NOT_FOUND(1244, "Không tìm thấy yêu cầu duyệt bài học", HttpStatus.NOT_FOUND),
    LESSON_REVIEW_SUBMIT_FORBIDDEN(1245, "Chỉ giáo viên được giao mới gửi bài học lên duyệt", HttpStatus.FORBIDDEN),
    LESSON_REVIEW_WITHDRAW_FORBIDDEN(1246, "Chỉ giáo viên đang được giao khóa học mới rút được yêu cầu duyệt bài", HttpStatus.FORBIDDEN),
    LESSON_REVIEW_COURSE_PENDING(1247, "Khóa học đang chờ duyệt theo cả khóa — cần xử lý yêu cầu đó trước", HttpStatus.CONFLICT),
    LESSON_REVIEW_EMPTY(1248, "Chưa chọn bài học nào để gửi duyệt", HttpStatus.BAD_REQUEST),
    LESSON_REVIEW_NOT_IN_COURSE(1249, "Bài học không thuộc khóa học này", HttpStatus.BAD_REQUEST),
    // Phiên sửa bài của ADMIN (lock Redis, Phase 4)
    LESSON_ADMIN_EDITING(1250, "Quản trị viên đang sửa bài này — bạn vẫn xem được nhưng chưa lưu / gửi duyệt được tới khi họ lưu hoặc hủy sửa", HttpStatus.CONFLICT),
    LESSON_EDIT_SESSION_REQUIRED(1251, "Phiên sửa bài chưa bắt đầu hoặc đã hết hạn — bấm Sửa để bắt đầu lại", HttpStatus.CONFLICT),
    LESSON_LOCKED_BY_OTHER_ADMIN(1252, "Quản trị viên khác đang sửa bài này — có thể tiếp quản nếu cần", HttpStatus.CONFLICT),
    LESSON_EDIT_LOCK_UNAVAILABLE(1253, "Tạm thời không khóa được bài để sửa, vui lòng thử lại sau", HttpStatus.SERVICE_UNAVAILABLE),
    // Autosave nội dung chưa lưu (Phase 5)
    LESSON_AUTOSAVE_TOO_LARGE(1254, "Bản chưa lưu quá lớn để tự lưu (tối đa 64 KB)", HttpStatus.BAD_REQUEST),
    LESSON_AUTOSAVE_KEY_INVALID(1255, "Mã form tự lưu không hợp lệ", HttpStatus.BAD_REQUEST),
    LESSON_AUTOSAVE_TOO_MANY(1256, "Bạn có quá nhiều bản chưa lưu trong bài này — hãy lưu hoặc bỏ bớt", HttpStatus.BAD_REQUEST),
    LESSON_NOT_IN_LIVE(1257, "Bài chưa có trong bản học viên đang học (chưa từng được duyệt) — không có bản để quay về", HttpStatus.BAD_REQUEST),
    // Nháp theo phía + khóa sửa theo khóa học (V65)
    COURSE_EDIT_SESSION_REQUIRED(1258, "Bạn chưa bắt đầu sửa khóa học hoặc phiên sửa đã hết hạn — bấm \"Sửa khóa học\" để bắt đầu lại", HttpStatus.CONFLICT),
    COURSE_EDIT_LOCKED_BY_OTHER(1259, "Người khác đang sửa khóa học này — có thể tiếp quản nếu cần", HttpStatus.CONFLICT),
    COURSE_EDIT_LOCK_UNAVAILABLE(1260, "Tạm thời không khóa được khóa học để sửa, vui lòng thử lại sau", HttpStatus.SERVICE_UNAVAILABLE),
    LESSON_DRAFT_OUTDATED(1261, "Bản học viên đang học đã thay đổi sau khi bản nháp này được tạo — hãy cập nhật bản mới", HttpStatus.CONFLICT),
    LESSON_DRAFT_NOT_FOUND(1262, "Không tìm thấy bản nháp của bài học", HttpStatus.NOT_FOUND),
    LESSON_SAVE_BLOCKED_BY_PENDING(1263, "Bài đang có yêu cầu duyệt của giáo viên — hãy duyệt / từ chối trước khi lưu bài", HttpStatus.CONFLICT),
    LESSON_DRAFT_NO_ARCHIVE(1264, "Bài này không có bản nháp cũ", HttpStatus.NOT_FOUND),
    COURSE_EDIT_BLOCKED_BY_PENDING(1265, "Khóa học còn bài chờ duyệt — hãy duyệt / từ chối hết trước khi sửa", HttpStatus.CONFLICT),

    // CURRICULUM MODULE — Enrollment (1300-1399)
    ENROLLMENT_NOT_FOUND(1300, "Không tìm thấy thông tin đăng ký khóa học", HttpStatus.NOT_FOUND),
    ALREADY_ENROLLED(1301, "Bạn đã đăng ký khóa học này rồi", HttpStatus.CONFLICT),
    NOT_ENROLLED(1302, "Người dùng chưa đăng ký khóa học này", HttpStatus.FORBIDDEN),

    // CURRICULUM MODULE — Progress (1400-1499)
    PROGRESS_NOT_FOUND(1400, "Không tìm thấy tiến độ bài học", HttpStatus.NOT_FOUND),
    PROGRESS_ALREADY_COMPLETED(1401, "Bài học đã được hoàn thành", HttpStatus.BAD_REQUEST),
    CANNOT_COMPLETE_LESSON(1402, "Chưa thể đánh dấu hoàn thành bài học này", HttpStatus.BAD_REQUEST),

    // CURRICULUM MODULE — Authorization (1500-1599)
    FORBIDDEN_COURSE_ACCESS(1500, "Bạn không có quyền truy cập khóa học này", HttpStatus.FORBIDDEN),
    FORBIDDEN_LESSON_ACCESS(1501, "Bạn không có quyền truy cập bài học này", HttpStatus.FORBIDDEN),
    FORBIDDEN_PROGRESS_ACCESS(1502, "Bạn không có quyền cập nhật tiến độ này", HttpStatus.FORBIDDEN),

    // CURRICULUM MODULE — Quiz (2000-2999)
    COURSE_NOT_ENROLLED(2002, "Bạn chưa đăng ký khóa học này. Hãy đăng ký để học bài và làm quiz!", HttpStatus.FORBIDDEN),
    QUIZ_NOT_FOUND(2005, "Bài học này chưa có quiz", HttpStatus.NOT_FOUND),
    QUESTION_NOT_FOUND(2006, "Không tìm thấy câu hỏi", HttpStatus.NOT_FOUND),
    QUESTION_ORDER_CONFLICT(2007, "Thứ tự này đã được dùng cho một câu hỏi khác trong quiz", HttpStatus.CONFLICT),

    // SRS Module (3000 - 3999)
    SRS_ITEM_NOT_FOUND(3001, "Không tìm thấy thẻ ôn tập", HttpStatus.NOT_FOUND),
    SRS_ITEM_ALREADY_EXISTS(3002, "Mục này đã có trong danh sách ôn tập SRS của bạn", HttpStatus.BAD_REQUEST),
    SRS_ITEM_ACCESS_DENIED(3003, "Bạn không có quyền truy cập thẻ ôn tập này", HttpStatus.FORBIDDEN),
    VOCAB_NOT_FOUND(3004, "Không tìm thấy từ vựng tương ứng", HttpStatus.NOT_FOUND),
    CHARACTER_NOT_FOUND(3005, "Không tìm thấy chữ Kanji tương ứng", HttpStatus.NOT_FOUND),
    GRAMMAR_NOT_FOUND(3006, "Không tìm thấy điểm ngữ pháp tương ứng", HttpStatus.NOT_FOUND),
    INVALID_SRS_REQUEST(3007, "Yêu cầu thêm vào SRS không hợp lệ (cần chọn từ vựng, Kanji hoặc ngữ pháp)", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(3008, "Dữ liệu gửi lên không hợp lệ", HttpStatus.BAD_REQUEST),
    SRS_ITEM_ARCHIVED(3009, "Thẻ ôn tập đang ở Lưu trữ, hãy khôi phục trước khi ôn", HttpStatus.BAD_REQUEST),

    // COMMENT MODULE (4000 - 4099)
    COMMENT_NOT_FOUND(4000, "Không tìm thấy bình luận", HttpStatus.NOT_FOUND),
    COMMENT_FORBIDDEN(4001, "Bạn không có quyền sửa bình luận này", HttpStatus.FORBIDDEN),
    COMMENT_INVALID_TARGET(4002, "Nội dung được bình luận không hợp lệ hoặc không tồn tại", HttpStatus.BAD_REQUEST),
    COMMENT_CONTENT_EMPTY(4003, "Nội dung bình luận không được để trống", HttpStatus.BAD_REQUEST),
    COMMENT_REPLY_NOT_FOUND(4004, "Không tìm thấy câu trả lời", HttpStatus.NOT_FOUND),
    COMMENT_REPLY_FORBIDDEN(4005, "Bạn không có quyền sửa câu trả lời này", HttpStatus.FORBIDDEN),
    COMMENT_REPLY_INVALID_PARENT(4006, "Câu trả lời gốc phải thuộc cùng một bình luận", HttpStatus.BAD_REQUEST),
    COMMENT_ALREADY_REPORTED(4007, "Bạn đã báo cáo nội dung này rồi", HttpStatus.CONFLICT),
    COMMENT_REPORT_OWN(4008, "Bạn không thể báo cáo nội dung của chính mình", HttpStatus.BAD_REQUEST),
    COMMENT_MODERATION_FORBIDDEN(4009, "Bạn không có quyền kiểm duyệt nội dung này", HttpStatus.FORBIDDEN),

    // EXAM MODULE (5000 - 5099)
    EXAM_NOT_FOUND(5000, "Không tìm thấy đề thi", HttpStatus.NOT_FOUND),
    EXAM_NOT_PUBLISHED(5001, "Đề thi chưa được xuất bản", HttpStatus.BAD_REQUEST),
    EXAM_SECTION_NOT_FOUND(5002, "Không tìm thấy phần thi", HttpStatus.NOT_FOUND),
    EXAM_ATTEMPT_NOT_FOUND(5003, "Không tìm thấy bài làm", HttpStatus.NOT_FOUND),
    EXAM_ATTEMPT_ALREADY_SUBMITTED(5004, "Bài làm này đã được nộp", HttpStatus.BAD_REQUEST),
    EXAM_ATTEMPT_FORBIDDEN(5005, "Bạn không có quyền truy cập bài làm này", HttpStatus.FORBIDDEN),
    EXAM_QUESTION_NOT_FOUND(5006, "Không tìm thấy câu hỏi trong đề thi", HttpStatus.NOT_FOUND),
    EXAM_OPTION_NOT_FOUND(5007, "Không tìm thấy phương án trả lời", HttpStatus.NOT_FOUND),
    EXAM_NOT_YET_APPROVED(5009, "Đề thi gắn khóa chưa được duyệt — đề sẽ lên cùng lần duyệt khóa học", HttpStatus.BAD_REQUEST),
    EXAM_HAS_IN_PROGRESS_ATTEMPTS(5008, "Không thể xóa câu hỏi hoặc đề thi khi đang có thí sinh làm bài trực tiếp", HttpStatus.CONFLICT),

    // AI Speaking Practice (6000 - 6099)
    AI_SESSION_NOT_FOUND(6000, "Không tìm thấy phiên luyện nói", HttpStatus.NOT_FOUND),
    AI_SCENARIO_NOT_FOUND(6001, "Không tìm thấy kịch bản luyện nói", HttpStatus.NOT_FOUND),
    AI_QUOTA_EXCEEDED(6002, "Bạn đã dùng hết lượt luyện nói hôm nay", HttpStatus.TOO_MANY_REQUESTS),
    AI_SESSION_FORBIDDEN(6003, "Bạn không có quyền truy cập phiên luyện nói này", HttpStatus.FORBIDDEN),
    AI_SCENARIO_FORBIDDEN(6004, "Bạn không có quyền quản lý kịch bản này", HttpStatus.FORBIDDEN),

    // LEADERBOARD / PROGRESS MODULE (5100 - 5199)
    LEADERBOARD_INVALID_WEEK(5101, "Tuần không hợp lệ. Định dạng đúng là YYYY-Www (vd: 2026-W42)", HttpStatus.BAD_REQUEST),
    LEADERBOARD_WEEK_IN_FUTURE(5102, "Tuần được chọn nằm trong tương lai", HttpStatus.BAD_REQUEST),
    LEADERBOARD_REFRESH_TOO_FREQUENT(5103, "Bạn làm mới quá nhanh. Vui lòng thử lại sau vài giây", HttpStatus.TOO_MANY_REQUESTS),
    LEADERBOARD_REBUILD_IN_PROGRESS(5104, "Bảng xếp hạng đang được cập nhật. Vui lòng thử lại sau ít phút", HttpStatus.CONFLICT),
    LEADERBOARD_REBUILD_FAILED(5105, "Không thể cập nhật bảng xếp hạng", HttpStatus.SERVICE_UNAVAILABLE),
    PROGRESS_GOAL_INVALID(5106, "Mục tiêu phải từ 30 đến 140 từ/tuần, theo bước 10", HttpStatus.BAD_REQUEST),

    // NOTIFICATION MODULE (6100 - 6199)
    NOTIFICATION_NOT_FOUND(6100, "Không tìm thấy thông báo", HttpStatus.NOT_FOUND),
    NOTIFICATION_FORBIDDEN(6101, "Bạn không có quyền truy cập thông báo này", HttpStatus.FORBIDDEN),
    BROADCAST_INVALID_AUDIENCE(6102, "Đối tượng nhận thông báo không hợp lệ", HttpStatus.BAD_REQUEST),
    BROADCAST_FORBIDDEN(6103, "Bạn không có quyền gửi thông báo tới đối tượng này", HttpStatus.FORBIDDEN),
    BROADCAST_NO_RECIPIENTS(6104, "Không có người nhận nào phù hợp với đối tượng đã chọn", HttpStatus.BAD_REQUEST),
    

    // NOTEBOOK MODULE (7000 - 7099)
    NOTEBOOK_NOT_FOUND(7000, "Không tìm thấy sổ tay", HttpStatus.NOT_FOUND),
    NOTEBOOK_FORBIDDEN(7001, "Bạn không có quyền truy cập sổ tay này", HttpStatus.FORBIDDEN),
    NOTEBOOK_GROUP_NOT_FOUND(7002, "Không tìm thấy nhóm sổ tay", HttpStatus.NOT_FOUND),
    NOTEBOOK_GROUP_NAME_EXISTED(7003, "Tên nhóm sổ tay đã tồn tại", HttpStatus.BAD_REQUEST),
    NOTEBOOK_SHARE_CODE_NOT_FOUND(7004, "Mã chia sẻ không tồn tại hoặc sổ tay đã tắt chia sẻ", HttpStatus.NOT_FOUND),
    NOTEBOOK_WORD_NOT_FOUND(7005, "Không tìm thấy từ trong sổ tay", HttpStatus.NOT_FOUND),
    NOTEBOOK_EXCEL_ROW_LIMIT_EXCEEDED(7006, "File Excel vượt quá giới hạn 500 dòng", HttpStatus.BAD_REQUEST),
    NOTEBOOK_EXCEL_EMPTY_FILE(7007, "File Excel không có dữ liệu", HttpStatus.BAD_REQUEST),
    NOTEBOOK_EXCEL_INVALID_FORMAT(7008, "File Excel không đúng định dạng", HttpStatus.BAD_REQUEST),
    NOTEBOOK_ALREADY_EXISTS(7009, "Sổ tay này đã có trong kho của bạn rồi", HttpStatus.BAD_REQUEST),
    NOTEBOOK_CANNOT_CLONE_OWN(7010, "Bạn là tác giả của sổ tay này và đã có trong kho cá nhân", HttpStatus.BAD_REQUEST),
    NOTEBOOK_ITEM_ALREADY_EXISTS(7011, "Mục này đã có trong sổ tay", HttpStatus.CONFLICT),
    NOTEBOOK_ITEM_SOURCE_NOT_FOUND(7012, "Không tìm thấy chữ Hán / ngữ pháp này trong từ điển", HttpStatus.NOT_FOUND),
    NOTEBOOK_LESSON_ITEMS_EMPTY(7013, "Không có mục nào để lưu vào sổ tay", HttpStatus.BAD_REQUEST),
    NOTEBOOK_LESSON_ITEMS_LIMIT_EXCEEDED(7014, "Mỗi lần chỉ lưu được tối đa 500 mục", HttpStatus.BAD_REQUEST),
    NOTEBOOK_TARGET_REQUIRED(7015, "Hãy chọn sổ tay hoặc nhập tên sổ tay mới", HttpStatus.BAD_REQUEST),
;


     int code;
     String message;
     HttpStatusCode httpStatusCode;

}