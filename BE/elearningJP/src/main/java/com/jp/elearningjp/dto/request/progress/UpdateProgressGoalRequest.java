package com.jp.elearningjp.dto.request.progress;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProgressGoalRequest {

    @NotNull(message = "Mục tiêu số từ mỗi tuần không được để trống")
    Integer weeklyGoalWords;
}
