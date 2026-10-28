package com.jp.elearningjp.service.speaking;

import com.jp.elearningjp.dto.response.speaking.AiSpeakingResponse;

import java.util.function.Consumer;

/**
 * Interface trừu tượng giao tiếp với các LLM (Gemini, Claude, GPT...).
 * Cho phép dễ dàng thay đổi hoặc đổi model trong tương lai mà không ảnh hưởng tầng service.
 */
public interface LlmClient {

    /**
     * Gửi prompt và nhận phản hồi dạng đối tượng chuẩn AiSpeakingResponse (synchronous).
     */
    AiSpeakingResponse generateResponse(LlmSpeakingPrompt prompt);

    /**
     * Gửi prompt và stream các mẩu câu trả lời tiếng Nhật về cho client theo thời gian thực (SSE),
     * sau đó hoàn tất đối tượng trọn vẹn AiSpeakingResponse.
     */
    void streamResponse(
            LlmSpeakingPrompt prompt,
            Consumer<String> chunkConsumer,
            Consumer<AiSpeakingResponse> onComplete,
            Consumer<Throwable> onError
    );
}
