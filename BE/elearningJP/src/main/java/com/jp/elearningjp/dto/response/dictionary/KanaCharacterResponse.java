package com.jp.elearningjp.dto.response.dictionary;

import com.jp.elearningjp.shared.enums.CharacterType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KanaCharacterResponse {
    Long id;
    String character;
    CharacterType charType;
    String romaji;
    String meaningVi;
    Integer strokeCount;
    String audioUrl;
    List<CharacterStrokeResponse> strokes;
}
