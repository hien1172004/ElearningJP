package com.jp.elearningjp.service.speaking;

import com.jp.elearningjp.dto.response.speaking.FuriganaResponse;

public interface FuriganaService {

    FuriganaResponse parseFurigana(String textJa);

    String toRubyHtml(String textJa);

    String toRomaji(String textJa);
}
