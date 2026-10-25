package com.jp.elearningjp.dto.response.speaking;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslateResponse {

    private Long messageId;

    private String originalText;

    private String translatedText;
}
