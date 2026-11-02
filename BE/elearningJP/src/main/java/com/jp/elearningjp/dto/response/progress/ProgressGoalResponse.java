package com.jp.elearningjp.dto.response.progress;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressGoalResponse {

    @Schema(description = "User đã tự chọn mục tiêu chưa. false → FE hiện trang chọn mục tiêu.")
    private boolean configured;

    @Schema(description = "Số từ mới mỗi tuần user đã chọn. null nếu chưa chọn.", example = "30")
    private Integer weeklyGoalWords;

    @Schema(description = "Số từ mới mỗi ngày đang áp dụng = ceil(tuần / 7). Chưa chọn thì là mặc định.", example = "5")
    private int dailyGoalWords;
}
