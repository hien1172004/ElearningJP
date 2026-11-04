package com.jp.elearningjp.controller.comment;

import com.jp.elearningjp.dto.request.comment.CommentReportRequest;
import com.jp.elearningjp.service.comment.CommentReportService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.API_V1)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Comment Report", description = "Báo cáo vi phạm bình luận / trả lời")
public class CommentReportController {

    CommentReportService commentReportService;

    @PostMapping(ApiPaths.Comment.REPORT)
    @Operation(summary = "Báo cáo vi phạm bình luận",
            description = "Mỗi người chỉ báo cáo một bình luận một lần; không tự báo cáo bình luận của mình")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> reportComment(@PathVariable Long id, @Valid @RequestBody CommentReportRequest request) {
        commentReportService.reportComment(id, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Đã gửi báo cáo. Cảm ơn bạn đã giúp giữ cộng đồng lành mạnh!")
                .build();
    }

    @PostMapping(ApiPaths.CommentReply.REPORT)
    @Operation(summary = "Báo cáo vi phạm trả lời",
            description = "Mỗi người chỉ báo cáo một trả lời một lần; không tự báo cáo trả lời của mình")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> reportReply(@PathVariable Long id, @Valid @RequestBody CommentReportRequest request) {
        commentReportService.reportReply(id, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Đã gửi báo cáo. Cảm ơn bạn đã giúp giữ cộng đồng lành mạnh!")
                .build();
    }
}
