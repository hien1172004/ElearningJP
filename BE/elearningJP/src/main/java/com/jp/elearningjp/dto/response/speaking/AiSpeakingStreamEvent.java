package com.jp.elearningjp.dto.response.speaking;

import lombok.*;

/**
 * Event trả về qua kênh SSE (Server-Sent Events) cho frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSpeakingStreamEvent {

    public enum EventType {
        CHUNK,      // Từng cụm ký tự câu thoại tiếng Nhật (progressive streaming)
        COMPLETE,   // Hoàn tất lượt thoại: trả trọn vẹn JSON (câu nói, sửa lỗi, nhiệm vụ hoàn thành, ID tin nhắn)
        ERROR       // Báo lỗi nếu gặp sự cố
    }

    private EventType type;

    private String chunk; // Dùng khi type = CHUNK

    private AiSpeakingResponse response; // Dùng khi type = COMPLETE

    private Long userMessageId; // ID tin nhắn người dùng đã lưu vào DB

    private Long aiMessageId; // ID tin nhắn AI đã lưu vào DB

    private String errorMessage; // Dùng khi type = ERROR
}
