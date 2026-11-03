package com.jp.elearningjp.controller.notebook;

import com.jp.elearningjp.dto.request.notebook.CreateNotebookGroupRequest;
import com.jp.elearningjp.dto.request.notebook.UpdateNotebookGroupRequest;
import com.jp.elearningjp.dto.response.notebook.NotebookGroupResponse;
import com.jp.elearningjp.dto.response.notebook.NotebookGroupWithNotebooksResponse;
import com.jp.elearningjp.service.notebook.NotebookGroupService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.NotebookGroup.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Notebook Groups", description = "Quản lý nhóm sổ tay từ vựng (thư mục gom sổ tay)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class NotebookGroupController {

    NotebookGroupService notebookGroupService;

    @Operation(summary = "Tạo nhóm sổ tay mới")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotebookGroupResponse> createGroup(
            @Valid @RequestBody CreateNotebookGroupRequest request
    ) {
        return ApiResponse.<NotebookGroupResponse>builder()
                .success(true)
                .data(notebookGroupService.createGroup(request))
                .build();
    }

    @Operation(summary = "Lấy danh sách nhóm sổ tay của tôi")
    @GetMapping
    public ApiResponse<List<NotebookGroupResponse>> getMyGroups() {
        return ApiResponse.<List<NotebookGroupResponse>>builder()
                .success(true)
                .data(notebookGroupService.getMyGroups())
                .build();
    }

    @Operation(summary = "Lấy danh sách nhóm kèm sổ tay bên trong")
    @GetMapping(ApiPaths.NotebookGroup.WITH_NOTEBOOKS)
    public ApiResponse<List<NotebookGroupWithNotebooksResponse>> getMyGroupsWithNotebooks() {
        return ApiResponse.<List<NotebookGroupWithNotebooksResponse>>builder()
                .success(true)
                .data(notebookGroupService.getMyGroupsWithNotebooks())
                .build();
    }

    @Operation(summary = "Chi tiết một nhóm sổ tay")
    @GetMapping(ApiPaths.NotebookGroup.BY_ID)
    public ApiResponse<NotebookGroupResponse> getGroupById(@PathVariable Long id) {
        return ApiResponse.<NotebookGroupResponse>builder()
                .success(true)
                .data(notebookGroupService.getGroupById(id))
                .build();
    }

    @Operation(summary = "Cập nhật nhóm sổ tay")
    @PutMapping(ApiPaths.NotebookGroup.BY_ID)
    public ApiResponse<NotebookGroupResponse> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNotebookGroupRequest request
    ) {
        return ApiResponse.<NotebookGroupResponse>builder()
                .success(true)
                .data(notebookGroupService.updateGroup(id, request))
                .build();
    }

    @Operation(summary = "Xóa nhóm sổ tay (các sổ tay sẽ trở về trạng thái chưa phân nhóm)")
    @DeleteMapping(ApiPaths.NotebookGroup.BY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteGroup(@PathVariable Long id) {
        notebookGroupService.deleteGroup(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xóa nhóm sổ tay thành công")
                .build();
    }
}

