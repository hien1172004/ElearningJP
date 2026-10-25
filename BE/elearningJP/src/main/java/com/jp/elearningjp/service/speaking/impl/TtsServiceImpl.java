package com.jp.elearningjp.service.speaking.impl;

import com.jp.elearningjp.dto.request.speaking.TtsRequest;
import com.jp.elearningjp.dto.response.speaking.TtsResponse;
import com.jp.elearningjp.entity.speaking.AiMessage;
import com.jp.elearningjp.repository.speaking.AiMessageRepository;
import com.jp.elearningjp.service.speaking.TtsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class TtsServiceImpl implements TtsService {

    private final AiMessageRepository messageRepository;

    @Value("${app.tts.cache-dir:uploads/tts_cache}")
    private String cacheDir;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .build();

        try {
            Path path = Paths.get(cacheDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created TTS audio cache directory at: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("Could not create TTS cache directory: {}", e.getMessage());
        }
    }

    @Override
    public byte[] synthesizeSpeech(String text, String voice) {
        if (text == null || text.isBlank()) {
            return new byte[0];
        }

        String voiceCode = (voice != null && !voice.isBlank()) ? voice.trim() : "ja";
        String hash = computeHash(text.trim(), voiceCode);

        // 1. Check disk cache
        byte[] cached = getCachedAudio(hash);
        if (cached != null && cached.length > 0) {
            return cached;
        }

        // 2. Synthesize via Google TTS
        try {
            String encodedText = URLEncoder.encode(text.trim(), StandardCharsets.UTF_8);
            java.net.URI uri = java.net.URI.create("https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=ja&q=" + encodedText);

            ResponseEntity<byte[]> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .toEntity(byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] audioBytes = response.getBody();
                saveToCache(hash, audioBytes);
                return audioBytes;
            }
        } catch (Exception e) {
            log.error("Error synthesizing speech via TTS service for text '{}': {}", text, e.getMessage());
        }

        return new byte[0];
    }

    @Override
    @Transactional
    public TtsResponse generateAudio(TtsRequest request) {
        String text = request.getText().trim();
        String voice = (request.getVoice() != null) ? request.getVoice() : "ja";
        String hash = computeHash(text, voice);

        boolean isCached = isFileCached(hash);
        if (!isCached) {
            synthesizeSpeech(text, voice);
            isCached = isFileCached(hash);
        }

        String audioUrl = "/api/v1/ai/tts/audio/" + hash + ".mp3";

        // Cập nhật audioUrl vào ai_message nếu có messageId
        if (request.getMessageId() != null) {
            messageRepository.findById(request.getMessageId()).ifPresent(msg -> {
                msg.setAudioUrl(audioUrl);
                messageRepository.save(msg);
            });
        }

        return TtsResponse.builder()
                .audioUrl(audioUrl)
                .hash(hash)
                .cached(isCached)
                .build();
    }

    @Override
    public byte[] getCachedAudio(String hash) {
        try {
            Path file = Paths.get(cacheDir, hash + ".mp3");
            if (Files.exists(file)) {
                return Files.readAllBytes(file);
            }
        } catch (IOException e) {
            log.warn("Error reading cached audio file for hash {}: {}", hash, e.getMessage());
        }
        return null;
    }

    private boolean isFileCached(String hash) {
        return Files.exists(Paths.get(cacheDir, hash + ".mp3"));
    }

    private void saveToCache(String hash, byte[] data) {
        try {
            Path target = Paths.get(cacheDir, hash + ".mp3");
            Files.write(target, data);
            log.debug("Saved TTS audio cache for hash: {}", hash);
        } catch (IOException e) {
            log.warn("Could not write audio to cache: {}", e.getMessage());
        }
    }

    private String computeHash(String text, String voice) {
        String key = voice + ":" + text;
        return DigestUtils.md5DigestAsHex(key.getBytes(StandardCharsets.UTF_8));
    }
}
