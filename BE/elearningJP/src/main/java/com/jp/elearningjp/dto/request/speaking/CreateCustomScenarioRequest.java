package com.jp.elearningjp.dto.request.speaking;

import com.jp.elearningjp.shared.enums.JlptLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCustomScenarioRequest {

    @NotBlank(message = "Vai của bạn không được để trống")
    @Size(max = 100, message = "Vai của bạn tối đa 100 ký tự")
    private String userRole;

    @NotBlank(message = "Vai của AI không được để trống")
    @Size(max = 100, message = "Vai của AI tối đa 100 ký tự")
    private String aiRole;

    @NotBlank(message = "Mô tả tình huống không được để trống")
    @Size(max = 200, message = "Mô tả tối đa 200 ký tự")
    private String description;

    @NotNull(message = "Trình độ không được để trống")
    private JlptLevel jlptLevel;

    private String title;
}
