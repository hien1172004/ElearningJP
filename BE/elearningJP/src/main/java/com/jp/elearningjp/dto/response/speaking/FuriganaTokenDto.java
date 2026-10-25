package com.jp.elearningjp.dto.response.speaking;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuriganaTokenDto {

    private String surface;

    private String furigana;

    private String romaji;

    private boolean isKanji;
}
