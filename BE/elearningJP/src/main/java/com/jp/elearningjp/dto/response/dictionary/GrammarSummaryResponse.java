package com.jp.elearningjp.dto.response.dictionary;

import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GrammarSummaryResponse {
    Long id;
    String title;
    String structure;
    JlptLevel jlptLevel;
    String meaningVi;
}
