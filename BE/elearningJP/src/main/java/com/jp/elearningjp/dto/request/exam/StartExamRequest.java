package com.jp.elearningjp.dto.request.exam;

import com.jp.elearningjp.shared.enums.AttemptType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StartExamRequest {
    @NotNull(message = "Attempt type is required")
    AttemptType attemptType;

    Long sectionId;
}
