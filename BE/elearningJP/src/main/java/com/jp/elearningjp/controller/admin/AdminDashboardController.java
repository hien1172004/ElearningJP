package com.jp.elearningjp.controller.admin;

import com.jp.elearningjp.dto.response.admin.AdminDashboardOverviewResponse;
import com.jp.elearningjp.service.admin.AdminDashboardService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.AdminDashboard.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin Dashboard", description = "Số liệu thống kê tổng quan dành cho Quản trị viên")
public class AdminDashboardController {

    AdminDashboardService adminDashboardService;

    @GetMapping(ApiPaths.AdminDashboard.OVERVIEW)
    @Operation(summary = "[Admin] Thống kê tổng quan",
            description = "Người dùng, mức độ hoạt động học tập và kết quả thi thử JLPT trong `days` ngày gần nhất "
                    + "(tính tới hết hôm nay, múi giờ Asia/Ho_Chi_Minh), kèm số liệu kỳ trước để so sánh. "
                    + "`days` trong khoảng 1–90, mặc định 30.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDashboardOverviewResponse> getOverview(
            @RequestParam(defaultValue = "30") int days
    ) {
        return ApiResponse.<AdminDashboardOverviewResponse>builder()
                .success(true)
                .data(adminDashboardService.getOverview(days))
                .build();
    }
}
