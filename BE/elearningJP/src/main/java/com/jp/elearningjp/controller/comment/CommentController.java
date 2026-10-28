package com.jp.elearningjp.controller.comment;

import com.jp.elearningjp.dto.request.comment.CommentCreateRequest;
import com.jp.elearningjp.dto.request.comment.CommentReplyCreateRequest;
import com.jp.elearningjp.dto.request.comment.CommentUpdateRequest;
import com.jp.elearningjp.dto.response.comment.CommentDetailResponse;
import com.jp.elearningjp.dto.response.comment.CommentListResponse;
import com.jp.elearningjp.dto.response.comment.CommentReactionResponse;
import com.jp.elearningjp.dto.response.comment.CommentReplyListResponse;
import com.jp.elearningjp.dto.response.comment.CommentReplyResponse;
import com.jp.elearningjp.dto.response.comment.CommentResponse;
import com.jp.elearningjp.service.comment.CommentService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.enums.CommentTargetType;
import com.jp.elearningjp.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cho module Comment — phục vụ bình luận trên bài học và tra cứu.
 *
 * <p>Endpoint theo target (dùng {@code targetType} trong path):
 * <ul>
 *   <li>{@code GET    /api/v1/{targetType}/{targetId}/comments} — danh sách (công khai)</li>
 *   <li>{@code POST   /api/v1/{targetType}/{targetId}/comments} — tạo mới (cần đăng nhập)</li>
 * </ul>
 *
 * <p>Endpoint theo id comment:
 * <ul>
 *   <li>{@code GET    /api/v1/comments/{id}} — chi tiết (công khai)</li>
 *   <li>{@code PUT    /api/v1/comments/{id}} — cập nhật (chỉ chủ sở hữu)</li>
 *   <li>{@code DELETE /api/v1/comments/{id}} — xoá mềm (chỉ chủ sở hữu)</li>
 *   <li>{@code POST   /api/v1/comments/{id}/like}    — toggle like</li>
 *   <li>{@code POST   /api/v1/comments/{id}/dislike} — toggle dislike</li>
 *   <li>{@code GET    /api/v1/comments/{id}/replies} — danh sách reply (công khai)</li>
 *   <li>{@code POST   /api/v1/comments/{id}/replies} — tạo reply (cần đăng nhập)</li>
 * </ul>
 *
 * <p>Endpoint theo id reply:
 * <ul>
 *   <li>{@code DELETE /api/v1/comment-replies/{id}} — xoá mềm (chỉ chủ sở hữu)</li>
 *   <li>{@code POST   /api/v1/comment-replies/{id}/like}    — toggle like reply</li>
 *   <li>{@code POST   /api/v1/comment-replies/{id}/dislike} — toggle dislike reply</li>
 * </ul>
 */
@Tag(name = "Comments", description = "Bình luận cho bài học + tra cứu (Vocabulary / Kanji / Grammar)")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentController {

    CommentService commentService;

    // ============================================================
    // READ
    // ============================================================

    @Operation(
            summary = "Danh sách comment của 1 target",
            description = "Trả về comment theo targetType (LESSON / VOCABULARY / KANJI / GRAMMAR) + targetId. Sắp xếp mới nhất trước, có phân trang."
    )
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Comment.BY_TARGET)
    public ApiResponse<CommentListResponse> getCommentsByTarget(
            @Parameter(description = "Loại đối tượng", example = "LESSON")
            @PathVariable("targetType") CommentTargetType targetType,
            @Parameter(description = "ID của đối tượng", example = "1")
            @PathVariable("targetId") Long targetId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.<CommentListResponse>builder()
                .success(true)
                .data(commentService.getCommentsByTarget(targetType, targetId, page, size))
                .build();
    }

    @Operation(
            summary = "Chi tiết 1 comment (kèm toàn bộ reply)",
            description = "Trả về 1 comment và danh sách reply (lồng nhau được làm phẳng, client tự dựng cây qua parentReplyId)."
    )
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Comment.BY_ID)
    public ApiResponse<CommentDetailResponse> getCommentDetail(
            @Parameter(description = "ID của comment", example = "1")
            @PathVariable("id") Long commentId
    ) {
        return ApiResponse.<CommentDetailResponse>builder()
                .success(true)
                .data(commentService.getCommentDetail(commentId))
                .build();
    }

    @Operation(summary = "Danh sách reply của 1 comment (phân trang)")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Comment.REPLIES)
    public ApiResponse<CommentReplyListResponse> getReplies(
            @PathVariable("id") Long commentId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size
    ) {
        return ApiResponse.<CommentReplyListResponse>builder()
                .success(true)
                .data(commentService.getRepliesByComment(commentId, page, size))
                .build();
    }

    // ============================================================
    // CREATE
    // ============================================================

    @Operation(
            summary = "[Người dùng] Tạo comment trên 1 target",
            description = "Bình luận lên bài học (LESSON) hoặc tra cứu (VOCABULARY / KANJI / GRAMMAR). Yêu cầu đăng nhập."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Comment.BY_TARGET)
    public ApiResponse<CommentResponse> createComment(
            @PathVariable("targetType") CommentTargetType targetType,
            @PathVariable("targetId") Long targetId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ApiResponse.<CommentResponse>builder()
                .success(true)
                .message("Đăng bình luận thành công")
                .data(commentService.createComment(targetType, targetId, request))
                .build();
    }

    @Operation(summary = "[Người dùng] Tạo reply cho 1 comment (có thể reply lồng nhau)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Comment.REPLIES)
    public ApiResponse<CommentReplyResponse> addReply(
            @PathVariable("id") Long commentId,
            @Valid @RequestBody CommentReplyCreateRequest request
    ) {
        return ApiResponse.<CommentReplyResponse>builder()
                .success(true)
                .message("Đăng reply thành công")
                .data(commentService.addReply(commentId, request))
                .build();
    }

    // ============================================================
    // UPDATE / DELETE
    // ============================================================

    @Operation(summary = "[Chủ sở hữu] Cập nhật nội dung comment")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping(ApiPaths.API_V1 + ApiPaths.Comment.BY_ID)
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable("id") Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return ApiResponse.<CommentResponse>builder()
                .success(true)
                .message("Cập nhật bình luận thành công")
                .data(commentService.updateComment(commentId, request))
                .build();
    }

    @Operation(summary = "[Chủ sở hữu] Xoá mềm comment")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Comment.BY_ID)
    public ApiResponse<Void> deleteComment(@PathVariable("id") Long commentId) {
        commentService.deleteComment(commentId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xoá bình luận")
                .build();
    }

    @Operation(summary = "[Chủ sở hữu] Xoá mềm reply")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.CommentReply.BY_ID)
    public ApiResponse<Void> deleteReply(@PathVariable("id") Long replyId) {
        commentService.deleteReply(replyId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xoá reply")
                .build();
    }

    // ============================================================
    // TOGGLE LIKE / DISLIKE
    // ============================================================

    @Operation(summary = "Toggle like / unlike cho comment (cần đăng nhập)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Comment.LIKE)
    public ApiResponse<CommentReactionResponse> toggleLike(@PathVariable("id") Long commentId) {
        CommentReactionResponse reaction = commentService.toggleLike(commentId);
        return ApiResponse.<CommentReactionResponse>builder()
                .success(true)
                .message(reaction.isLiked() ? "Đã thích bình luận" : "Đã bỏ thích bình luận")
                .data(reaction)
                .build();
    }

    @Operation(summary = "Toggle dislike / undislike cho comment (cần đăng nhập)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Comment.DISLIKE)
    public ApiResponse<CommentReactionResponse> toggleDislike(@PathVariable("id") Long commentId) {
        CommentReactionResponse reaction = commentService.toggleDislike(commentId);
        return ApiResponse.<CommentReactionResponse>builder()
                .success(true)
                .message(reaction.isDisliked() ? "Đã đánh giá không hữu ích" : "Đã bỏ đánh giá")
                .data(reaction)
                .build();
    }

    @Operation(summary = "Toggle like / unlike cho reply (cần đăng nhập)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.CommentReply.LIKE)
    public ApiResponse<CommentReactionResponse> toggleReplyLike(@PathVariable("id") Long replyId) {
        CommentReactionResponse reaction = commentService.toggleReplyLike(replyId);
        return ApiResponse.<CommentReactionResponse>builder()
                .success(true)
                .message(reaction.isLiked() ? "Đã thích reply" : "Đã bỏ thích reply")
                .data(reaction)
                .build();
    }

    @Operation(summary = "Toggle dislike / undislike cho reply (cần đăng nhập)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.CommentReply.DISLIKE)
    public ApiResponse<CommentReactionResponse> toggleReplyDislike(@PathVariable("id") Long replyId) {
        CommentReactionResponse reaction = commentService.toggleReplyDislike(replyId);
        return ApiResponse.<CommentReactionResponse>builder()
                .success(true)
                .message(reaction.isDisliked() ? "Đã đánh giá không hữu ích" : "Đã bỏ đánh giá")
                .data(reaction)
                .build();
    }
}
