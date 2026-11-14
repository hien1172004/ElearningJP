package com.jp.elearningjp.controller.comment;

import com.jp.elearningjp.dto.request.comment.ModerationActionRequest;
import com.jp.elearningjp.dto.response.comment.ModerationItemResponse;
import com.jp.elearningjp.service.comment.CommentModerationService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.enums.CommentTargetType;
import com.jp.elearningjp.shared.enums.ContentStatus;
import com.jp.elearningjp.shared.enums.ReportStatus;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Kiểm duyệt bình luận. ADMIN: mọi bình luận; TEACHER: bình luận trong bài học thuộc khóa
 * được giao (kiểm tra ở service, ngoài phạm vi → 403).
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.AdminModeration.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Comment Moderation", description = "Kiểm duyệt bình luận (Quản trị viên: tất cả; Giáo viên: bình luận trong bài học của khóa mình)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class CommentModerationController {

    CommentModerationService moderationService;

    @GetMapping(ApiPaths.AdminModeration.REPORTS)
    @Operation(summary = "[Admin/Teacher] Hàng chờ báo cáo vi phạm",
            description = "Gom báo cáo theo bình luận/trả lời, nhiều báo cáo nhất lên đầu. status: OPEN (mặc định) | RESOLVED | REJECTED; "
                    + "keyword: lọc theo nội dung, tên hoặc email người viết")
    public ApiResponse<PageResponse<ModerationItemResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<ModerationItemResponse>>builder()
                .success(true)
                .data(moderationService.getReportQueue(status, keyword, page, size))
                .build();
    }

    @GetMapping(ApiPaths.AdminModeration.COMMENTS)
    @Operation(summary = "[Admin/Teacher] Tra cứu bình luận",
            description = "kind: COMMENT | REPLY, bỏ trống = gộp cả bình luận và trả lời (phân trang chung theo thời gian); "
                    + "status: VISIBLE | HIDDEN; lọc theo loại nội dung và từ khóa (nội dung, tên, email người viết)")
    public ApiResponse<PageResponse<ModerationItemResponse>> browse(
            @RequestParam(required = false) ModerationItemResponse.Kind kind,
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) CommentTargetType targetType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<ModerationItemResponse>>builder()
                .success(true)
                .data(moderationService.browse(kind, status, targetType, keyword, page, size))
                .build();
    }

    // ----------------------------- Bình luận gốc -----------------------------

    @PostMapping(ApiPaths.AdminModeration.COMMENT_HIDE)
    @Operation(summary = "[Admin/Teacher] Ẩn bình luận", description = "Gỡ khỏi trang công khai (khôi phục được), đóng các báo cáo và thông báo cho người viết")
    public ApiResponse<Void> hideComment(@PathVariable Long id, @Valid @RequestBody(required = false) ModerationActionRequest request) {
        moderationService.hideComment(id, note(request));
        return ok("Đã ẩn bình luận");
    }

    @PostMapping(ApiPaths.AdminModeration.COMMENT_RESTORE)
    @Operation(summary = "[Admin/Teacher] Khôi phục bình luận đã ẩn")
    public ApiResponse<Void> restoreComment(@PathVariable Long id) {
        moderationService.restoreComment(id);
        return ok("Đã khôi phục bình luận");
    }

    @DeleteMapping(ApiPaths.AdminModeration.COMMENT_BY_ID)
    @Operation(summary = "[Admin/Teacher] Xóa bình luận", description = "Xóa mềm, đóng các báo cáo và thông báo cho người viết")
    public ApiResponse<Void> deleteComment(@PathVariable Long id, @Valid @RequestBody(required = false) ModerationActionRequest request) {
        moderationService.deleteComment(id, note(request));
        return ok("Đã xóa bình luận");
    }

    @PostMapping(ApiPaths.AdminModeration.COMMENT_DISMISS)
    @Operation(summary = "[Admin/Teacher] Bỏ qua báo cáo của bình luận", description = "Đánh dấu các báo cáo đang mở là không vi phạm (REJECTED)")
    public ApiResponse<Void> dismissCommentReports(@PathVariable Long id) {
        moderationService.dismissCommentReports(id);
        return ok("Đã bỏ qua báo cáo");
    }

    // ----------------------------- Trả lời -----------------------------

    @PostMapping(ApiPaths.AdminModeration.REPLY_HIDE)
    @Operation(summary = "[Admin/Teacher] Ẩn trả lời", description = "Gỡ khỏi trang công khai (khôi phục được), đóng các báo cáo và thông báo cho người viết")
    public ApiResponse<Void> hideReply(@PathVariable Long id, @Valid @RequestBody(required = false) ModerationActionRequest request) {
        moderationService.hideReply(id, note(request));
        return ok("Đã ẩn trả lời");
    }

    @PostMapping(ApiPaths.AdminModeration.REPLY_RESTORE)
    @Operation(summary = "[Admin/Teacher] Khôi phục trả lời đã ẩn")
    public ApiResponse<Void> restoreReply(@PathVariable Long id) {
        moderationService.restoreReply(id);
        return ok("Đã khôi phục trả lời");
    }

    @DeleteMapping(ApiPaths.AdminModeration.REPLY_BY_ID)
    @Operation(summary = "[Admin/Teacher] Xóa trả lời", description = "Xóa mềm trả lời cùng các trả lời con, đóng báo cáo và thông báo cho người viết")
    public ApiResponse<Void> deleteReply(@PathVariable Long id, @Valid @RequestBody(required = false) ModerationActionRequest request) {
        moderationService.deleteReply(id, note(request));
        return ok("Đã xóa trả lời");
    }

    @PostMapping(ApiPaths.AdminModeration.REPLY_DISMISS)
    @Operation(summary = "[Admin/Teacher] Bỏ qua báo cáo của trả lời", description = "Đánh dấu các báo cáo đang mở là không vi phạm (REJECTED)")
    public ApiResponse<Void> dismissReplyReports(@PathVariable Long id) {
        moderationService.dismissReplyReports(id);
        return ok("Đã bỏ qua báo cáo");
    }

    private static String note(ModerationActionRequest request) {
        return request != null ? request.getNote() : null;
    }

    private static ApiResponse<Void> ok(String message) {
        return ApiResponse.<Void>builder().success(true).message(message).build();
    }
}
