package com.jp.elearningjp.service.speaking.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.response.speaking.AiSpeakingResponse;
import com.jp.elearningjp.entity.speaking.AiMessage;
import com.jp.elearningjp.service.speaking.LlmClient;
import com.jp.elearningjp.service.speaking.LlmSpeakingPrompt;
import com.jp.elearningjp.shared.enums.AiMessageRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation của LlmClient sử dụng Google Gemini REST API.
 */
@Component
@Slf4j
public class GeminiLlmClient implements LlmClient {

    private final String apiKey;
    private final String model;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}";
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");

    private static final List<String> DEFAULT_FALLBACK_MODELS = List.of(
            "gemini-3.5-flash-lite",
            "gemini-2.5-flash",
            "gemini-3.5-flash",
            "gemini-2.0-flash"
    );

    public GeminiLlmClient(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.5-flash-lite}") String model,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = (model != null && !model.isBlank()) ? model.trim() : "gemini-3.5-flash-lite";
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    private List<String> getCandidateModels() {
        List<String> list = new ArrayList<>();
        if (model != null && !model.isBlank()) {
            list.add(model.trim());
        }
        for (String m : DEFAULT_FALLBACK_MODELS) {
            if (!list.contains(m)) {
                list.add(m);
            }
        }
        return list;
    }

    @Override
    public AiSpeakingResponse generateResponse(LlmSpeakingPrompt prompt) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình GEMINI_API_KEY trong hệ thống!");
        }

        Map<String, Object> requestBody = buildGeminiPayload(prompt);
        List<String> modelsToTry = getCandidateModels();

        for (String currentModel : modelsToTry) {
            try {
                String rawResponse = restClient.post()
                        .uri(GEMINI_API_URL, currentModel, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                AiSpeakingResponse parsed = parseGeminiResponse(rawResponse);
                if (parsed != null && parsed.getReplyJa() != null && !parsed.getReplyJa().isBlank()) {
                    log.info("Gemini phản hồi thành công qua model: {}", currentModel);
                    return parsed;
                }
            } catch (Exception e) {
                log.warn("Model {} bị quá tải hoặc gặp sự cố ({}). Đang tự động thử model dự phòng tiếp theo...", currentModel, e.getMessage());
            }
        }

        // Nếu tất cả các model đều quá tải hoặc mất mạng quốc tế, trả về phản hồi fallback thông minh, không văng lỗi làm ngắt trải nghiệm học viên
        log.warn("Tất cả model Gemini đều không khả dụng. Trả về lượt thoại thông minh dự phòng.");
        return AiSpeakingResponse.builder()
                .replyJa("はい、よく分かりました！そのことについて、もっと詳しく教えていただけますか？")
                .replyVi("Vâng, tôi hiểu rồi! Bạn có thể chia sẻ thêm chi tiết về điều đó được không?")
                .build();
    }

    @Override
    public void streamResponse(
            LlmSpeakingPrompt prompt,
            Consumer<String> chunkConsumer,
            Consumer<AiSpeakingResponse> onComplete,
            Consumer<Throwable> onError
    ) {
        try {
            // Lấy phản hồi chuẩn đã validate JSON từ Gemini
            AiSpeakingResponse response = generateResponse(prompt);
            String replyJa = response.getReplyJa();

            // Stream từng cụm ký tự tiếng Nhật (progressive streaming) mô phỏng hội thoại trực tiếp
            if (replyJa != null && !replyJa.isEmpty()) {
                int chunkSize = 2; // Gửi 2 ký tự mỗi lượt
                for (int i = 0; i < replyJa.length(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, replyJa.length());
                    String piece = replyJa.substring(i, end);
                    chunkConsumer.accept(piece);
                    try {
                        Thread.sleep(40); // 40ms delay tạo hiệu ứng gõ mượt mà
                    } catch (InterruptedException ignored) {}
                }
            }

            // Hoàn tất trả về trọn vẹn kết quả
            onComplete.accept(response);
        } catch (Throwable t) {
            log.error("Lỗi trong quá trình stream lượt thoại AI: {}", t.getMessage(), t);
            onError.accept(t);
        }
    }

    private Map<String, Object> buildGeminiPayload(LlmSpeakingPrompt prompt) {
        Map<String, Object> payload = new HashMap<>();

        // System Instruction
        Map<String, Object> systemInstruction = new HashMap<>();
        systemInstruction.put("parts", List.of(Map.of("text", prompt.getSystemInstruction())));
        payload.put("system_instruction", systemInstruction);

        // Contents (lịch sử trò chuyện + câu nói mới)
        List<Map<String, Object>> contents = new ArrayList<>();

        if (prompt.getRecentHistory() != null) {
            for (AiMessage msg : prompt.getRecentHistory()) {
                String role = (msg.getRole() == AiMessageRole.USER) ? "user" : "model";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.getTextJa()))
                ));
            }
        }

        // Câu nói mới nhất của người học
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", prompt.getUserNewMessage()))
        ));
        payload.put("contents", contents);

        // Cấu hình sinh JSON
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("response_mime_type", "application/json");
        generationConfig.put("temperature", 0.7);
        generationConfig.put("max_output_tokens", 1024);
        payload.put("generationConfig", generationConfig);

        return payload;
    }

    private AiSpeakingResponse parseGeminiResponse(String geminiRawJson) throws Exception {
        JsonNode root = objectMapper.readTree(geminiRawJson);

        // Trích xuất text từ candidates[0].content.parts[0].text
        JsonNode candidates = root.path("candidates");
        if (candidates.isMissingNode() || !candidates.isArray() || candidates.isEmpty()) {
            throw new RuntimeException("Gemini trả về danh sách candidates rỗng");
        }

        String rawContent = candidates.get(0).path("content").path("parts").get(0).path("text").asText("");
        int totalTokens = root.path("usageMetadata").path("totalTokenCount").asInt(0);

        // Làm sạch chuỗi JSON nếu có bao quanh bởi markdown ```json ... ```
        String cleanedJson = cleanJsonString(rawContent);

        AiSpeakingResponse response = objectMapper.readValue(cleanedJson, AiSpeakingResponse.class);
        response.setTokensUsed(totalTokens);
        return response;
    }

    private String cleanJsonString(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();

        Matcher matcher = JSON_BLOCK_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return trimmed.substring(start, end + 1).trim();
        }

        return trimmed;
    }
}
