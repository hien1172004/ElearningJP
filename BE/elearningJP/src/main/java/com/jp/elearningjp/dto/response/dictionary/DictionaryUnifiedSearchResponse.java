package com.jp.elearningjp.dto.response.dictionary;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DictionaryUnifiedSearchResponse {
    String keyword;
    List<VocabularySummaryResponse> vocabularies;
    List<KanjiSummaryResponse> kanjis;
    List<GrammarSummaryResponse> grammars;
}
