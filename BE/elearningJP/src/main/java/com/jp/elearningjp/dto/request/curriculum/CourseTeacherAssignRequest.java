package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** ADMIN giao một giáo viên cho khóa học. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseTeacherAssignRequest {

    @NotNull(message = "ID giáo viên không được để trống")
    Long teacherId;
}
