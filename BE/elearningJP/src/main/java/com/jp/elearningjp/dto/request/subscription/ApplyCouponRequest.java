package com.jp.elearningjp.dto.request.subscription;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyCouponRequest {

    @NotBlank(message = "Vui lòng nhập mã giảm giá")
    private String code;

    @NotBlank(message = "Vui lòng chọn gói cước")
    private String planId;
}

