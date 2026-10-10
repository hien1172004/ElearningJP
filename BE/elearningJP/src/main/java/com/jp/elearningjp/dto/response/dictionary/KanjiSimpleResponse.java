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
public class KanjiSimpleResponse {
    Long id;
    String character;
    String hanViet;
    String meaningVi;
    Integer strokeCount;
    List<String> onyomi;
    List<String> kunyomi;
    JlptLevel jlptLevel;
}
