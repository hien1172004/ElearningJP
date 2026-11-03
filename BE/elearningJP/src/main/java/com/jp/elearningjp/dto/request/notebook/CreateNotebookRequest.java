package com.jp.elearningjp.dto.request.notebook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateNotebookRequest {

    @NotBlank(message = "Tên sổ tay không được để trống")
    @Size(max = 100, message = "Tên sổ tay không được vượt quá 100 ký tự")
    String name;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    String description;

    Long groupId;

    @Size(max = 20, message = "Mã màu không được vượt quá 20 ký tự")
    String coverColor;

    @Size(max = 50, message = "Mã icon không được vượt quá 50 ký tự")
    String coverIcon;

    @Pattern(regexp = "^(N[1-5])?$", message = "Cấp độ JLPT phải là N1, N2, N3, N4 hoặc N5")
    String jlptLevel;

    Boolean isPublic;
}

