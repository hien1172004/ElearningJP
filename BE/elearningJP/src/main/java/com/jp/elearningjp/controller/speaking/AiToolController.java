package com.jp.elearningjp.controller.speaking;

import com.jp.elearningjp.dto.request.speaking.FuriganaRequest;
import com.jp.elearningjp.dto.request.speaking.TranslateRequest;
import com.jp.elearningjp.dto.request.speaking.TtsRequest;
import com.jp.elearningjp.dto.response.speaking.FuriganaResponse;
import com.jp.elearningjp.dto.response.speaking.TranslateResponse;
import com.jp.elearningjp.dto.response.speaking.TtsResponse;
import com.jp.elearningjp.service.speaking.FuriganaService;
import com.jp.elearningjp.service.speaking.TranslationService;
import com.jp.elearningjp.service.redis.RateLimitService;
import com.jp.elearningjp.service.speaking.TtsService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Speaking Tools", description = "APIs hỗ trợ luyện nói: Furigana/Romaji qua Kuromoji, TTS phát âm, dịch câu Nhật - Việt")
public class AiToolController {

    private final FuriganaService furiganaService;
    private final TtsService ttsService;
    private final TranslationService translationService;
    private final RateLimitService rateLimitService;
    private final CurrentUserService currentUserService;

    // Furigana chạy Kuromoji cục bộ (rẻ, FE gọi cho mọi tin nhắn) nên không giới hạn
    static final int TTS_MAX_PER_USER_PER_MINUTE = 60;
    static final int TRANSLATE_MAX_PER_USER_PER_MINUTE = 60;
    static final int TTS_STREAM_MAX_PER_IP_PER_MINUTE = 60;

    @PostMapping("/furigana")
    @Operation(summary = "Phân tích hình thái câu tiếng Nhật (Kuromoji) sinh Furigana, Romaji và thẻ <ruby>")
    public ApiResponse<FuriganaResponse> parseFurigana(@Valid @RequestBody FuriganaRequest request) {
        FuriganaResponse response = furiganaService.parseFurigana(request.getText());
        return ApiResponse.<FuriganaResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .build();
    }

    @PostMapping("/tts")
    @Operation(summary = "Tạo audio phát âm cho câu tiếng Nhật, cache theo hash(text + voice)")
    public ApiResponse<TtsResponse> generateTts(@Valid @RequestBody TtsRequest request) {
        rateLimitService.check("ai:tts:" + currentUserService.getCurrentUserId(),
                TTS_MAX_PER_USER_PER_MINUTE, Duration.ofMinutes(1));
        TtsResponse response = ttsService.generateAudio(request);
        return ApiResponse.<TtsResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Tạo âm thanh phát âm thành công")
                .data(response)
                .build();
    }

    @GetMapping(value = "/tts/audio/{filename:[a-f0-9]{32}\\.mp3}", produces = "audio/mpeg")
    @Operation(summary = "Tải hoặc phát trực tiếp file audio đã cache")
    public ResponseEntity<byte[]> getCachedAudio(@PathVariable String filename) {
        String hash = filename.substring(0, 32);
        byte[] audio = ttsService.getCachedAudio(hash);

        if (audio == null || audio.length == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(audio);
    }

    @GetMapping(value = "/tts/stream", produces = "audio/mpeg")
    @Operation(summary = "Phát âm thanh trực tiếp (stream audio/mpeg)")
    public ResponseEntity<byte[]> streamSpeech(
            @RequestParam String text,
            @RequestParam(defaultValue = "ja") String voice,
            HttpServletRequest http) {
        // API công khai (thẻ <audio>) → đếm theo IP; trả 429 trống vì endpoint chỉ sinh audio/mpeg, không trả JSON lỗi được
        if (!rateLimitService.tryAcquire("ai:tts-stream:ip:" + http.getRemoteAddr(),
                TTS_STREAM_MAX_PER_IP_PER_MINUTE, Duration.ofMinutes(1))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        byte[] audio = ttsService.synthesizeSpeech(text, voice);
        if (audio == null || audio.length == 0) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(audio);
    }

    @PostMapping("/translate")
    @Operation(summary = "Dịch câu tiếng Nhật sang tiếng Việt và lưu vào tin nhắn (text_vi)")
    public ApiResponse<TranslateResponse> translate(@Valid @RequestBody TranslateRequest request) {
        rateLimitService.check("ai:translate:" + currentUserService.getCurrentUserId(),
                TRANSLATE_MAX_PER_USER_PER_MINUTE, Duration.ofMinutes(1));
        TranslateResponse response = translationService.translate(request);
        return ApiResponse.<TranslateResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Dịch câu thành công")
                .data(response)
                .build();
    }
}
