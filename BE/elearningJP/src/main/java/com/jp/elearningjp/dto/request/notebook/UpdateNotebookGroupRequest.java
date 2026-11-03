package com.jp.elearningjp.dto.request.notebook;

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
public class UpdateNotebookGroupRequest {

    @Pattern(regexp = "^(?!\\s*$).+", message = "Tên nhóm không được chỉ chứa khoảng trắng")
    @Size(max = 100, message = "Tên nhóm không được vượt quá 100 ký tự")
    String name;

    Integer displayOrder;
}

