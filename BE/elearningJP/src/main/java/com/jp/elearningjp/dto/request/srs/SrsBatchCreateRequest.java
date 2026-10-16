package com.jp.elearningjp.dto.request.srs;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SrsBatchCreateRequest {
    Long lessonId;
    List<Long> vocabIds;
    List<Long> characterIds;
    List<Long> grammarIds;
}
