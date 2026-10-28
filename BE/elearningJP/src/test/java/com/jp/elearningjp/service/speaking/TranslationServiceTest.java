package com.jp.elearningjp.service.speaking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.request.speaking.TranslateRequest;
import com.jp.elearningjp.dto.response.speaking.TranslateResponse;
import com.jp.elearningjp.entity.speaking.AiMessage;
import com.jp.elearningjp.repository.speaking.AiMessageRepository;
import com.jp.elearningjp.service.speaking.impl.TranslationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private AiMessageRepository messageRepository;

    private TranslationServiceImpl translationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        translationService = new TranslationServiceImpl(messageRepository, objectMapper);
        translationService.init();
    }

    @Test
    @DisplayName("Dịch câu có sẵn text_vi trong DB trả về ngay lập tức không cần gọi API ngoài")
    void testTranslate_WithCachedTextVi() {
        AiMessage message = AiMessage.builder()
                .textJa("こんにちは")
                .textVi("Xin chào")
                .build();
        message.setId(10L);

        when(messageRepository.findById(10L)).thenReturn(Optional.of(message));

        TranslateRequest request = TranslateRequest.builder()
                .messageId(10L)
                .build();

        TranslateResponse response = translationService.translate(request);

        assertNotNull(response);
        assertEquals(10L, response.getMessageId());
        assertEquals("こんにちは", response.getOriginalText());
        assertEquals("Xin chào", response.getTranslatedText());

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dịch câu chưa có text_vi thì thực hiện dịch và lưu vào DB")
    void testTranslate_WhenNoTextViInDb() {
        AiMessage message = AiMessage.builder()
                .textJa("おはようございます")
                .build();
        message.setId(20L);

        when(messageRepository.findById(20L)).thenReturn(Optional.of(message));

        TranslateRequest request = TranslateRequest.builder()
                .messageId(20L)
                .build();

        TranslateResponse response = translationService.translate(request);

        assertNotNull(response);
        assertEquals(20L, response.getMessageId());
        assertEquals("おはようございます", response.getOriginalText());
        assertNotNull(response.getTranslatedText());

        verify(messageRepository, times(1)).save(any(AiMessage.class));
    }
}
