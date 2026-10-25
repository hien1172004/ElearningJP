package com.jp.elearningjp.service.speaking.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.request.speaking.TranslateRequest;
import com.jp.elearningjp.dto.response.speaking.TranslateResponse;
import com.jp.elearningjp.entity.speaking.AiMessage;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.speaking.AiMessageRepository;
import com.jp.elearningjp.service.speaking.TranslationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationServiceImpl implements TranslationService {

    private final AiMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.5-flash}")
    private String geminiModel;

    private RestClient restClient;

    private final java.util.Map<String, String> translationCache = new java.util.concurrent.ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/122.0.0.0")
                .build();
    }

    @Override
    @Transactional
    public TranslateResponse translate(TranslateRequest request) {
        String textToTranslate = request.getText();
        AiMessage message = null;

        if (request.getMessageId() != null) {
            message = messageRepository.findById(request.getMessageId()).orElse(null);
            if (message != null) {
                // Nếu tin nhắn đã có bản dịch sẵn trong DB, trả về ngay lập tức
                if (message.getTextVi() != null && !message.getTextVi().isBlank()) {
                    return TranslateResponse.builder()
                            .messageId(message.getId())
                            .originalText(message.getTextJa())
                            .translatedText(message.getTextVi())
                            .build();
                }
                if (textToTranslate == null || textToTranslate.isBlank()) {
                    textToTranslate = message.getTextJa();
                }
            }
        }

        if (textToTranslate == null || textToTranslate.isBlank()) {
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }

        String translated = translateJapaneseToVietnamese(textToTranslate.trim());

        // Lưu bản dịch vào ai_message nếu có
        if (message != null) {
            message.setTextVi(translated);
            messageRepository.save(message);
        }

        return TranslateResponse.builder()
                .messageId(request.getMessageId())
                .originalText(textToTranslate.trim())
                .translatedText(translated)
                .build();
    }

    @Override
    public String translateJapaneseToVietnamese(String textJa) {
        if (textJa == null || textJa.isBlank()) {
            return "";
        }

        String key = textJa.trim();
        String cached = translationCache.get(key);
        if (cached != null) {
            return cached;
        }

        // 1. Dùng Gemini LLM để dịch chuẩn xác theo ngữ cảnh hội thoại (tắt thinking token để phản hồi siêu tốc)
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                String prompt = "Dịch câu tiếng Nhật sau sang tiếng Việt tự nhiên và chuẩn xác. Chỉ trả về duy nhất 1 dòng câu dịch tiếng Việt, tuyệt đối không có lời dẫn dắt, không có giải thích hay chú thích: " + key;
                java.net.URI geminiUri = java.net.URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + geminiApiKey);

                Map<String, Object> body = Map.of(
                        "contents", java.util.List.of(
                                Map.of("parts", java.util.List.of(Map.of("text", prompt)))
                        ),
                        "generationConfig", Map.of(
                                "temperature", 0.1,
                                "maxOutputTokens", 120,
                                "thinkingConfig", Map.of("thinkingBudget", 0)
                        )
                );

                String geminiResponse = restClient.post()
                        .uri(geminiUri)
                        .header("Content-Type", "application/json")
                        .body(body)
                        .retrieve()
                        .body(String.class);

                if (geminiResponse != null) {
                    JsonNode root = objectMapper.readTree(geminiResponse);
                    JsonNode candidate = root.path("candidates").path(0);
                    JsonNode parts = candidate.path("content").path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        for (JsonNode part : parts) {
                            if (!part.path("thought").asBoolean(false) && part.has("text")) {
                                String rawText = part.path("text").asText().trim();
                                if (!rawText.isBlank()) {
                                    // Lấy dòng đầu tiên nếu model trả về nhiều dòng
                                    String[] lines = rawText.split("\n");
                                    for (String line : lines) {
                                        String clean = line.replaceAll("^[\"“*\\-\\s]+|[\"”*\\-\\s]+$", "").trim();
                                        if (!clean.isEmpty()) {
                                            translationCache.put(key, clean);
                                            return clean;
                                        }
                                    }
                                    String clean = rawText.replaceAll("^\"|\"$", "").trim();
                                    translationCache.put(key, clean);
                                    return clean;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Gemini translation error, falling back to Google: {}", e.getMessage());
            }
        }

        // 2. Dự phòng: Thử dịch qua Google Translate API
        try {
            String encoded = URLEncoder.encode(textJa.trim(), StandardCharsets.UTF_8);
            java.net.URI googleUri = java.net.URI.create("https://translate.googleapis.com/translate_a/single?client=gtx&sl=ja&tl=vi&dt=t&q=" + encoded);

            String response = restClient.get()
                    .uri(googleUri)
                    .retrieve()
                    .body(String.class);

            if (response != null && response.startsWith("[")) {
                JsonNode root = objectMapper.readTree(response);
                if (root.isArray() && root.size() > 0 && root.get(0).isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode segment : root.get(0)) {
                        if (segment.isArray() && segment.size() > 0) {
                            sb.append(segment.get(0).asText());
                        }
                    }
                    String result = sb.toString().trim();
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Google translate fallback error: {}", e.getMessage());
        }

        return textJa;
    }
}
