package com.jp.elearningjp.controller.speaking;

import com.jp.elearningjp.dto.request.speaking.AdminScenarioRequest;
import com.jp.elearningjp.dto.response.speaking.AdminScenarioResponse;
import com.jp.elearningjp.service.speaking.AdminScenarioService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.response.PageResponse;
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
@RequestMapping(ApiPaths.API_V1 + ApiPaths.AdminScenario.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin Scenario", description = "APIs quản lý kịch bản luyện nói AI (Quản trị viên: tất cả; Giáo viên: kịch bản của mình)")
public class AdminScenarioController {

    AdminScenarioService adminScenarioService;

    @GetMapping
    @Operation(summary = "[Admin/Teacher] Danh sách kịch bản luyện nói",
            description = "Quản trị viên thấy mọi kịch bản; giáo viên chỉ thấy kịch bản do mình tạo. "
                    + "Lọc theo từ khóa (tiêu đề/mô tả), chủ đề, cấp độ JLPT. Trang bắt đầu từ 1.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<PageResponse<AdminScenarioResponse>> getScenarios(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String topicId,
            @RequestParam(required = false) JlptLevel jlptLevel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<AdminScenarioResponse>>builder()
                .success(true)
                .data(adminScenarioService.getScenarios(keyword, topicId, jlptLevel, page, size))
                .build();
    }

    @GetMapping(ApiPaths.AdminScenario.BY_ID)
    @Operation(summary = "[Admin/Teacher] Xem chi tiết kịch bản", description = "Giáo viên chỉ xem được kịch bản do mình tạo")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<AdminScenarioResponse> getScenario(@PathVariable Long id) {
        return ApiResponse.<AdminScenarioResponse>builder()
                .success(true)
                .data(adminScenarioService.getScenario(id))
                .build();
    }

    @PostMapping
    @Operation(summary = "[Admin/Teacher] Tạo kịch bản luyện nói",
            description = "Tạo kịch bản đầy đủ nội dung (nhiệm vụ, hội thoại mẫu, từ vựng, ngữ pháp, mẹo); người tạo là tài khoản hiện tại")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<AdminScenarioResponse> createScenario(@Valid @RequestBody AdminScenarioRequest request) {
        return ApiResponse.<AdminScenarioResponse>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Tạo kịch bản thành công")
                .data(adminScenarioService.createScenario(request))
                .build();
    }

    @PutMapping(ApiPaths.AdminScenario.BY_ID)
    @Operation(summary = "[Admin/Teacher] Cập nhật kịch bản", description = "Ghi đè toàn bộ nội dung kịch bản. Giáo viên chỉ sửa được kịch bản do mình tạo")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<AdminScenarioResponse> updateScenario(
            @PathVariable Long id,
            @Valid @RequestBody AdminScenarioRequest request
    ) {
        return ApiResponse.<AdminScenarioResponse>builder()
                .success(true)
                .message("Cập nhật kịch bản thành công")
                .data(adminScenarioService.updateScenario(id, request))
                .build();
    }

    @DeleteMapping(ApiPaths.AdminScenario.BY_ID)
    @Operation(summary = "[Admin/Teacher] Xóa kịch bản",
            description = "Xóa hẳn kịch bản; các phiên luyện nói cũ vẫn được giữ. Giáo viên chỉ xóa được kịch bản do mình tạo")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Void> deleteScenario(@PathVariable Long id) {
        adminScenarioService.deleteScenario(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xóa kịch bản")
                .build();
    }
}
