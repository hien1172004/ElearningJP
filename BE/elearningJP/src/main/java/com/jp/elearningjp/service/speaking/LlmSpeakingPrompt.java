package com.jp.elearningjp.service.speaking;

import com.jp.elearningjp.entity.speaking.AiMessage;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Đóng gói toàn bộ prompt đầu vào gửi tới LLM:
 * - systemInstruction: Hướng dẫn hệ thống, vai trò, quy tắc, kịch bản, nhiệm vụ
 * - recentHistory: 10 tin nhắn gần nhất
 * - userNewMessage: Tin nhắn mới của người học
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmSpeakingPrompt {

    private String systemInstruction;

    @Builder.Default
    private List<AiMessage> recentHistory = new ArrayList<>();

    private String userNewMessage;
}
