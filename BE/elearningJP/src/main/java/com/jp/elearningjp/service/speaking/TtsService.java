package com.jp.elearningjp.service.speaking;

import com.jp.elearningjp.dto.request.speaking.TtsRequest;
import com.jp.elearningjp.dto.response.speaking.TtsResponse;

public interface TtsService {

    byte[] synthesizeSpeech(String text, String voice);

    TtsResponse generateAudio(TtsRequest request);

    byte[] getCachedAudio(String hash);
}
