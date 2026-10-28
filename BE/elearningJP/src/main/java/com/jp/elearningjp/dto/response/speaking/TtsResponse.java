package com.jp.elearningjp.dto.response.speaking;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TtsResponse {

    private String audioUrl;

    private String hash;

    private boolean cached;
}
