package com.jp.elearningjp.service.speaking;

import com.jp.elearningjp.dto.request.speaking.CreateSessionRequest;
import com.jp.elearningjp.dto.request.speaking.SendMessageRequest;
import com.jp.elearningjp.dto.response.speaking.AiSessionResponse;
import com.jp.elearningjp.dto.response.speaking.AiSpeakingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiSpeakingService {

    AiSessionResponse createSession(Long userId, CreateSessionRequest request);

    void sendMessageStream(Long userId, Long sessionId, SendMessageRequest request, SseEmitter emitter);

    AiSpeakingResponse sendMessageSync(Long userId, Long sessionId, SendMessageRequest request);

    AiSessionResponse getSession(Long userId, Long sessionId);

    Page<AiSessionResponse> getUserSessions(Long userId, Pageable pageable);

    void deleteSession(Long userId, Long sessionId);

    void deleteAllSessions(Long userId);
}
