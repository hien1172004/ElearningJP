package com.jp.elearningjp.dto.response.dictionary;

import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KanjiSummaryResponse {
    Long id;
    String character;
    Integer strokeCount;
    List<String> onyomi;
    List<String> kunyomi;
    String hanViet;
    String meaningVi;
    JlptLevel jlptLevel;
    List<String> radicals;
}
