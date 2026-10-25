package com.jp.elearningjp.service.speaking;

import com.jp.elearningjp.dto.request.speaking.TranslateRequest;
import com.jp.elearningjp.dto.response.speaking.TranslateResponse;

public interface TranslationService {

    TranslateResponse translate(TranslateRequest request);

    String translateJapaneseToVietnamese(String textJa);
}
