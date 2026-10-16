package com.jp.elearningjp.dto.response.dictionary;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GrammarDetailResponse {
    Long id;
    String title;
    String structure;
    JlptLevel jlptLevel;
    String meaningVi;
    String usageNotes;
    JsonNode examplesJson;
}
