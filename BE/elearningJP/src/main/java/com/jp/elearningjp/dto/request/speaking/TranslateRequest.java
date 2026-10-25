package com.jp.elearningjp.dto.request.speaking;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslateRequest {

    private Long messageId;

    private String text;
}
