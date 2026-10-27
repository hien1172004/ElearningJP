package com.jp.elearningjp.dto.request.user;

import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.LearnerLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    String fullName;

    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$|^$", message = "Số điện thoại không đúng định dạng")
    String phoneNumber;

    @Size(max = 500, message = "Tiểu sử tối đa 500 ký tự")
    String bio;

    LearnerLevel currentLevel;

    JlptLevel targetLevel;

    LocalDate targetExamDate;

    @Min(value = 5, message = "Thời gian học tối thiểu 5 phút/ngày")
    @Max(value = 480, message = "Thời gian học tối đa 480 phút/ngày")
    Integer dailyStudyTimeMinutes;
}
