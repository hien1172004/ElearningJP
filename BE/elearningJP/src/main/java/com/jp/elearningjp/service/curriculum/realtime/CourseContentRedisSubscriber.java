package com.jp.elearningjp.service.curriculum.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Nhận tín hiệu từ kênh Redis {@link CourseContentPublisher#CHANNEL} và gửi cho phiên STOMP của instance này. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseContentRedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final CourseContentPublisher publisher;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            CourseContentPublisher.ContentChangedMessage payload = objectMapper.readValue(
                    new String(message.getBody(), StandardCharsets.UTF_8),
                    CourseContentPublisher.ContentChangedMessage.class);
            publisher.deliverLocal(payload);
        } catch (Exception ex) {
            log.warn("[COURSE-CONTENT] Invalid message on Redis channel: {}", ex.getMessage());
        }
    }
}
