package com.jp.elearningjp.dto.response.speaking;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuriganaResponse {

    private String original;

    private String rubyHtml;

    private String romaji;

    @Builder.Default
    private List<FuriganaTokenDto> tokens = new ArrayList<>();
}
