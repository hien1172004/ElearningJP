package com.jp.elearningjp.controller.notebook;

import com.jp.elearningjp.dto.request.notebook.CreateNotebookRequest;
import com.jp.elearningjp.dto.request.notebook.UpdateNotebookRequest;
import com.jp.elearningjp.dto.response.notebook.NotebookResponse;
import com.jp.elearningjp.service.notebook.NotebookService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Notebook.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Notebooks", description = "Quản lý Sổ tay từ vựng cá nhân")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class NotebookController {

    NotebookService notebookService;

    @Operation(summary = "Tạo sổ tay mới")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotebookResponse> createNotebook(
            @Valid @RequestBody CreateNotebookRequest request
    ) {
        return ApiResponse.<NotebookResponse>builder()
                .success(true)
                .data(notebookService.createNotebook(request))
                .build();
    }

    @Operation(summary = "Lấy toàn bộ sổ tay của tôi (danh sách phẳng)")
    @GetMapping
    public ApiResponse<List<NotebookResponse>> getMyNotebooks() {
        return ApiResponse.<List<NotebookResponse>>builder()
                .success(true)
                .data(notebookService.getMyNotebooks())
                .build();
    }

    @Operation(summary = "Lấy các sổ tay chưa phân nhóm của tôi")
    @GetMapping(ApiPaths.Notebook.UNGROUPED)
    public ApiResponse<List<NotebookResponse>> getMyUngroupedNotebooks() {
        return ApiResponse.<List<NotebookResponse>>builder()
                .success(true)
                .data(notebookService.getMyUngroupedNotebooks())
                .build();
    }

    @Operation(summary = "Lấy danh sách sổ tay theo nhóm")
    @GetMapping(ApiPaths.Notebook.BY_GROUP)
    public ApiResponse<List<NotebookResponse>> getMyNotebooksByGroup(@PathVariable Long groupId) {
        return ApiResponse.<List<NotebookResponse>>builder()
                .success(true)
                .data(notebookService.getMyNotebooksByGroup(groupId))
                .build();
    }

    private static final java.util.Set<String> ALLOWED_SORT_FIELDS = java.util.Set.of(
            "id", "name", "createdAt", "updatedAt", "wordCount", "jlptLevel", "likeCount", "viewCount", "downloadCount"
    );

    @Operation(summary = "Tìm kiếm, lọc và phân trang sổ tay của tôi")
    @GetMapping(ApiPaths.Notebook.SEARCH)
    public ApiResponse<PageResponse<NotebookResponse>> searchMyNotebooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jlptLevel,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            sortField = "id";
        }
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 50), Sort.by(direction, sortField));
        Page<NotebookResponse> pageResult = notebookService.searchMyNotebooks(keyword, jlptLevel, groupId, pageRequest);

        PageResponse<NotebookResponse> pageResponse = PageResponse.<NotebookResponse>builder()
                .items(pageResult.getContent())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();

        return ApiResponse.<PageResponse<NotebookResponse>>builder()
                .success(true)
                .data(pageResponse)
                .build();
    }

    @Operation(summary = "Chi tiết một sổ tay")
    @GetMapping(ApiPaths.Notebook.BY_ID)
    public ApiResponse<NotebookResponse> getNotebookDetail(@PathVariable Long id) {
        return ApiResponse.<NotebookResponse>builder()
                .success(true)
                .data(notebookService.getNotebookDetail(id))
                .build();
    }

    @Operation(summary = "Cập nhật sổ tay")
    @PutMapping(ApiPaths.Notebook.BY_ID)
    public ApiResponse<NotebookResponse> updateNotebook(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNotebookRequest request
    ) {
        return ApiResponse.<NotebookResponse>builder()
                .success(true)
                .data(notebookService.updateNotebook(id, request))
                .build();
    }

    @Operation(summary = "Xóa sổ tay (soft delete)")
    @DeleteMapping(ApiPaths.Notebook.BY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteNotebook(@PathVariable Long id) {
        notebookService.deleteNotebook(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xóa sổ tay thành công")
                .build();
    }

    @Operation(summary = "Chuyển sổ tay sang nhóm khác (hoặc bỏ nhóm)")
    @PatchMapping(ApiPaths.Notebook.MOVE)
    public ApiResponse<NotebookResponse> moveToGroup(
            @PathVariable Long id,
            @RequestParam(required = false) Long targetGroupId
    ) {
        return ApiResponse.<NotebookResponse>builder()
                .success(true)
                .data(notebookService.moveToGroup(id, targetGroupId))
                .build();
    }

    @Operation(summary = "Bật / Tắt tính năng chia sẻ sổ tay qua mã 6 ký tự")
    @PatchMapping(ApiPaths.Notebook.SHARE_TOGGLE)
    public ApiResponse<NotebookResponse> toggleSharing(
            @PathVariable Long id,
            @RequestParam boolean enabled
    ) {
        return ApiResponse.<NotebookResponse>builder()
                .success(true)
                .data(notebookService.toggleSharing(id, enabled))
                .build();
    }

    @Operation(summary = "Bật / Tắt công khai sổ tay lên Khám phá cộng đồng")
    @PatchMapping(ApiPaths.Notebook.PUBLIC_TOGGLE)
    public ApiResponse<NotebookResponse> togglePublic(
            @PathVariable Long id,
            @RequestParam boolean isPublic
    ) {
        return ApiResponse.<NotebookResponse>builder()
                .success(true)
                .data(notebookService.togglePublic(id, isPublic))
                .build();
    }

    @Operation(summary = "Xem trước thông tin sổ tay qua mã chia sẻ 6 ký tự")
    @GetMapping(ApiPaths.Notebook.SHARE_BY_CODE)
    public ApiResponse<NotebookResponse> getNotebookByShareCode(@PathVariable String shareCode) {
        return ApiResponse.<NotebookResponse>builder()
                .success(true)
                .data(notebookService.getNotebookByShareCode(shareCode))
                .build();
    }

    @Operation(summary = "Nhân bản sổ tay (Deep Clone) vào kho cá nhân qua mã chia sẻ (Reset sạch SRS)")
    @PostMapping(ApiPaths.Notebook.CLONE_BY_CODE)
    public ApiResponse<NotebookResponse> cloneNotebookByShareCode(
            @RequestParam String shareCode,
            @RequestParam(required = false) Long targetGroupId
    ) {
        return ApiResponse.<NotebookResponse>builder()
                .success(true)
                .data(notebookService.cloneNotebookByShareCode(shareCode, targetGroupId))
                .build();
    }

    @Operation(summary = "Khám phá sổ tay cộng đồng (lọc theo Tất cả, Tải nhiều, Thích nhiều, Xem nhiều)")
    @GetMapping(ApiPaths.Notebook.EXPLORE)
    public ApiResponse<PageResponse<NotebookResponse>> exploreNotebooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "all") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 50));
        Page<NotebookResponse> pageResult = notebookService.exploreNotebooks(keyword, sort, pageRequest);

        return ApiResponse.<PageResponse<NotebookResponse>>builder()
                .success(true)
                .data(PageResponse.<NotebookResponse>builder()
                        .items(pageResult.getContent())
                        .page(pageResult.getNumber())
                        .size(pageResult.getSize())
                        .totalElements(pageResult.getTotalElements())
                        .totalPages(pageResult.getTotalPages())
                        .build())
                .build();
    }

    @Operation(summary = "Xem chi tiết một sổ tay cộng đồng (+1 view count)")
    @GetMapping(ApiPaths.Notebook.EXPLORE_BY_ID)
    public ApiResponse<NotebookResponse> viewPublicNotebookDetail(@PathVariable Long id) {
        return ApiResponse.<NotebookResponse>builder()
                .success(true)
                .data(notebookService.viewPublicNotebookDetail(id))
                .build();
    }

    @Operation(summary = "Thích / Bỏ thích một sổ tay cộng đồng")
    @PostMapping(ApiPaths.Notebook.EXPLORE_LIKE)
    public ApiResponse<Boolean> toggleLikeNotebook(@PathVariable Long id) {
        return ApiResponse.<Boolean>builder()
                .success(true)
                .data(notebookService.toggleLikeNotebook(id))
                .build();
    }

    @Operation(summary = "Lấy danh sách sổ tay cộng đồng yêu thích của tôi")
    @GetMapping(ApiPaths.Notebook.EXPLORE_FAVORITES)
    public ApiResponse<PageResponse<NotebookResponse>> getFavoriteNotebooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 50));
        Page<NotebookResponse> pageResult = notebookService.getFavoriteNotebooks(pageRequest);

        return ApiResponse.<PageResponse<NotebookResponse>>builder()
                .success(true)
                .data(PageResponse.<NotebookResponse>builder()
                        .items(pageResult.getContent())
                        .page(pageResult.getNumber())
                        .size(pageResult.getSize())
                        .totalElements(pageResult.getTotalElements())
                        .totalPages(pageResult.getTotalPages())
                        .build())
                .build();
    }

    @Operation(summary = "Lấy danh sách sổ tay cộng đồng đã xem gần đây của tôi")
    @GetMapping(ApiPaths.Notebook.EXPLORE_RECENT)
    public ApiResponse<PageResponse<NotebookResponse>> getRecentlyViewedNotebooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 50));
        Page<NotebookResponse> pageResult = notebookService.getRecentlyViewedNotebooks(pageRequest);

        return ApiResponse.<PageResponse<NotebookResponse>>builder()
                .success(true)
                .data(PageResponse.<NotebookResponse>builder()
                        .items(pageResult.getContent())
                        .page(pageResult.getNumber())
                        .size(pageResult.getSize())
                        .totalElements(pageResult.getTotalElements())
                        .totalPages(pageResult.getTotalPages())
                        .build())
                .build();
    }

}
