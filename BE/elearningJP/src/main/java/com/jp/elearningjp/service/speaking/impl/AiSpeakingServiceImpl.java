package com.jp.elearningjp.service.speaking.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jp.elearningjp.dto.request.speaking.CreateSessionRequest;
import com.jp.elearningjp.dto.request.speaking.SendMessageRequest;
import com.jp.elearningjp.dto.response.speaking.AiMessageResponse;
import com.jp.elearningjp.dto.response.speaking.AiSessionResponse;
import com.jp.elearningjp.dto.response.speaking.AiSpeakingResponse;
import com.jp.elearningjp.dto.response.speaking.AiSpeakingStreamEvent;
import com.jp.elearningjp.entity.speaking.AiMessage;
import com.jp.elearningjp.entity.speaking.AiScenario;
import com.jp.elearningjp.entity.speaking.AiSession;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.speaking.AiMessageRepository;
import com.jp.elearningjp.repository.speaking.AiScenarioRepository;
import com.jp.elearningjp.repository.speaking.AiSessionRepository;
import com.jp.elearningjp.repository.speaking.AiUsageRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.dto.request.speaking.TranslateRequest;
import com.jp.elearningjp.service.speaking.AiSpeakingService;
import com.jp.elearningjp.service.speaking.LlmClient;
import com.jp.elearningjp.service.speaking.LlmSpeakingPrompt;
import com.jp.elearningjp.service.speaking.PromptBuilderService;
import com.jp.elearningjp.service.speaking.TranslationService;
import com.jp.elearningjp.service.speaking.mapper.AiSpeakingMapper;
import com.jp.elearningjp.shared.enums.AiMessageRole;
import com.jp.elearningjp.shared.enums.AiPracticeMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiSpeakingServiceImpl implements AiSpeakingService {

    private final AiSessionRepository sessionRepository;
    private final AiMessageRepository messageRepository;
    private final AiScenarioRepository scenarioRepository;
    private final AiUsageRepository usageRepository;
    private final UserRepository userRepository;
    private final PromptBuilderService promptBuilderService;
    private final TranslationService translationService;
    private final LlmClient llmClient;
    private final AiSpeakingMapper speakingMapper;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public AiSessionResponse createSession(Long userId, CreateSessionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        AiSession session;
        String initialGreetingJa;
        String initialGreetingVi;

        if (request.getMode() == AiPracticeMode.SCENARIO) {
            if (request.getScenarioId() == null) {
                throw new AppException(ErrorCode.AI_SCENARIO_NOT_FOUND);
            }
            AiScenario scenario = scenarioRepository.findById(request.getScenarioId())
                    .orElseThrow(() -> new AppException(ErrorCode.AI_SCENARIO_NOT_FOUND));

            session = AiSession.builder()
                    .user(user)
                    .scenario(scenario)
                    .mode(AiPracticeMode.SCENARIO)
                    .title(scenario.getTitle())
                    .completedTasks(objectMapper.createArrayNode())
                    .build();
            session = sessionRepository.save(session);

            String[] greetingPair = extractInitialScenarioGreeting(scenario);
            initialGreetingJa = greetingPair[0];
            initialGreetingVi = greetingPair[1];
        } else {
            String topic = (request.getTopic() != null && !request.getTopic().isBlank())
                    ? request.getTopic().trim()
                    : "日常会話";

            session = AiSession.builder()
                    .user(user)
                    .mode(AiPracticeMode.FREE)
                    .title(topic)
                    .completedTasks(objectMapper.createArrayNode())
                    .build();
            session = sessionRepository.save(session);

            initialGreetingJa = "こんにちは！マジーコです。今日は「" + topic + "」について楽しくお話ししましょう！最近はどうですか？";
            initialGreetingVi = "Xin chào! Mình là Majiko. Hôm nay chúng ta hãy cùng trò chuyện vui vẻ về chủ đề「" + topic + "」nhé! Dạo này bạn thế nào rồi?";
        }

        AiMessage greetingMessage = AiMessage.builder()
                .session(session)
                .role(AiMessageRole.AI)
                .textJa(initialGreetingJa)
                .textVi(initialGreetingVi)
                .build();
        greetingMessage = messageRepository.save(greetingMessage);

        return speakingMapper.toSessionResponse(session, List.of(greetingMessage));
    }

    private String[] extractInitialScenarioGreeting(AiScenario scenario) {
        if (scenario.getSampleDialogue() != null && scenario.getSampleDialogue().isArray() && !scenario.getSampleDialogue().isEmpty()) {
            for (JsonNode line : scenario.getSampleDialogue()) {
                String role = line.path("role").asText("");
                String speaker = line.path("speaker").asText("");
                if (role.equalsIgnoreCase("AI") || role.equalsIgnoreCase(scenario.getAiRole()) ||
                    speaker.equalsIgnoreCase(scenario.getAiRole()) || speaker.equalsIgnoreCase("AI")) {
                    String ja = line.has("ja") ? line.get("ja").asText() : (line.has("text") ? line.get("text").asText() : "");
                    String vi = line.has("vi") ? line.get("vi").asText() : (line.has("meaning") ? line.get("meaning").asText() : "");
                    if (!ja.isBlank()) {
                        return new String[]{ja, vi};
                    }
                }
            }
            JsonNode firstLine = scenario.getSampleDialogue().get(0);
            String ja = firstLine.has("ja") ? firstLine.get("ja").asText() : (firstLine.has("text") ? firstLine.get("text").asText() : "");
            String vi = firstLine.has("vi") ? firstLine.get("vi").asText() : (firstLine.has("meaning") ? firstLine.get("meaning").asText() : "");
            if (!ja.isBlank()) {
                return new String[]{ja, vi};
            }
        }
        return new String[]{"いらっしゃいませ！こんにちは。よろしくお願いします。", "Kính chào quý khách! Xin chào. Rất mong được giúp đỡ."};
    }

    @Override
    public void sendMessageStream(Long userId, Long sessionId, SendMessageRequest request, SseEmitter emitter) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        // 1. Validate session & save user's message inside transaction
        final AiMessage savedUserMessage = txTemplate.execute(status -> {
            AiSession session = sessionRepository.findById(sessionId)
                    .filter(s -> !s.isDeleted())
                    .orElseThrow(() -> new AppException(ErrorCode.AI_SESSION_NOT_FOUND));

            if (!session.getUser().getId().equals(userId)) {
                throw new AppException(ErrorCode.AI_SESSION_FORBIDDEN);
            }

            AiMessage userMsg = AiMessage.builder()
                    .session(session)
                    .role(AiMessageRole.USER)
                    .textJa(request.getMessage().trim())
                    .build();
            return messageRepository.save(userMsg);
        });

        // Tự động dịch ngầm tin nhắn người dùng ở background (0ms khi bấm xem dịch)
        CompletableFuture.runAsync(() -> {
            try {
                if (savedUserMessage != null && savedUserMessage.getTextJa() != null && !savedUserMessage.getTextJa().isBlank()) {
                    translationService.translate(TranslateRequest.builder()
                            .messageId(savedUserMessage.getId())
                            .text(savedUserMessage.getTextJa())
                            .build());
                }
            } catch (Exception e) {
                log.warn("Auto-translate user message in background failed: {}", e.getMessage());
            }
        });

        // 2. Fetch session and recent history
        AiSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_SESSION_NOT_FOUND));

        List<AiMessage> recentDesc = messageRepository.findRecentMessagesBySessionId(sessionId, 10);
        List<AiMessage> recentHistory = new ArrayList<>(recentDesc);
        Collections.reverse(recentHistory);

        // 3. Build prompt
        LlmSpeakingPrompt prompt = promptBuilderService.buildPrompt(session, recentHistory, request.getMessage().trim());

        // 4. Stream via LLM client
        llmClient.streamResponse(
                prompt,
                chunk -> {
                    try {
                        AiSpeakingStreamEvent chunkEvent = AiSpeakingStreamEvent.builder()
                                .type(AiSpeakingStreamEvent.EventType.CHUNK)
                                .chunk(chunk)
                                .build();
                        emitter.send(SseEmitter.event().data(chunkEvent));
                    } catch (IOException e) {
                        log.warn("Client disconnected during SSE streaming: {}", e.getMessage());
                    }
                },
                aiResponse -> {
                    try {
                        // Persist AI message and update tasks in a new transaction
                        AiMessage savedAiMsg = txTemplate.execute(status -> {
                            AiSession s = sessionRepository.findById(sessionId).orElseThrow();

                            AiMessage aiMsg = AiMessage.builder()
                                    .session(s)
                                    .role(AiMessageRole.AI)
                                    .textJa(aiResponse.getReplyJa())
                                    .textVi(aiResponse.getReplyVi())
                                    .correction(aiResponse.getCorrection() != null ? objectMapper.valueToTree(aiResponse.getCorrection()) : null)
                                    .build();
                            aiMsg = messageRepository.save(aiMsg);

                            // Update completed tasks for scenario
                            if (s.getMode() == AiPracticeMode.SCENARIO && aiResponse.getCompletedTaskIds() != null) {
                                updateCompletedTasks(s, aiResponse.getCompletedTaskIds());
                            }

                            // Record daily token usage
                            int tokens = (aiResponse.getTokensUsed() != null && aiResponse.getTokensUsed() > 0)
                                    ? aiResponse.getTokensUsed() : 100;
                            usageRepository.recordUsage(userId, LocalDate.now(), tokens);

                            return aiMsg;
                        });

                        AiSpeakingStreamEvent completeEvent = AiSpeakingStreamEvent.builder()
                                .type(AiSpeakingStreamEvent.EventType.COMPLETE)
                                .response(aiResponse)
                                .userMessageId(savedUserMessage.getId())
                                .aiMessageId(savedAiMsg != null ? savedAiMsg.getId() : null)
                                .build();

                        emitter.send(SseEmitter.event().data(completeEvent));
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("Error finalizing AI speaking message: {}", e.getMessage(), e);
                        try {
                            emitter.send(SseEmitter.event().data(AiSpeakingStreamEvent.builder()
                                    .type(AiSpeakingStreamEvent.EventType.ERROR)
                                    .errorMessage(e.getMessage())
                                    .build()));
                        } catch (IOException ignored) {}
                        emitter.completeWithError(e);
                    }
                },
                throwable -> {
                    log.error("Error from LLM during streaming: {}", throwable.getMessage(), throwable);
                    try {
                        emitter.send(SseEmitter.event().data(AiSpeakingStreamEvent.builder()
                                .type(AiSpeakingStreamEvent.EventType.ERROR)
                                .errorMessage("Không thể nhận phản hồi từ AI: " + throwable.getMessage())
                                .build()));
                    } catch (IOException ignored) {}
                    emitter.complete();
                }
        );
    }

    @Override
    @Transactional
    public AiSpeakingResponse sendMessageSync(Long userId, Long sessionId, SendMessageRequest request) {
        AiSession session = sessionRepository.findById(sessionId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.AI_SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.AI_SESSION_FORBIDDEN);
        }

        // Save user message
        AiMessage userMsg = AiMessage.builder()
                .session(session)
                .role(AiMessageRole.USER)
                .textJa(request.getMessage().trim())
                .build();
        final AiMessage savedUserMsg = messageRepository.save(userMsg);

        // Tự động dịch ngầm tin nhắn người dùng ở background (0ms khi bấm xem dịch)
        CompletableFuture.runAsync(() -> {
            try {
                translationService.translate(TranslateRequest.builder()
                        .messageId(savedUserMsg.getId())
                        .text(savedUserMsg.getTextJa())
                        .build());
            } catch (Exception e) {
                log.warn("Auto-translate user message in background failed: {}", e.getMessage());
            }
        });

        // Fetch recent messages
        List<AiMessage> recentDesc = messageRepository.findRecentMessagesBySessionId(sessionId, 10);
        List<AiMessage> recentHistory = new ArrayList<>(recentDesc);
        Collections.reverse(recentHistory);

        // Prompt & invoke LLM
        LlmSpeakingPrompt prompt = promptBuilderService.buildPrompt(session, recentHistory, request.getMessage().trim());
        AiSpeakingResponse aiResponse = llmClient.generateResponse(prompt);

        // Save AI message
        AiMessage aiMsg = AiMessage.builder()
                .session(session)
                .role(AiMessageRole.AI)
                .textJa(aiResponse.getReplyJa())
                .textVi(aiResponse.getReplyVi())
                .correction(aiResponse.getCorrection() != null ? objectMapper.valueToTree(aiResponse.getCorrection()) : null)
                .build();
        messageRepository.save(aiMsg);

        // Update tasks
        if (session.getMode() == AiPracticeMode.SCENARIO && aiResponse.getCompletedTaskIds() != null) {
            updateCompletedTasks(session, aiResponse.getCompletedTaskIds());
        }

        // Record tokens
        int tokens = (aiResponse.getTokensUsed() != null && aiResponse.getTokensUsed() > 0)
                ? aiResponse.getTokensUsed() : 100;
        usageRepository.recordUsage(userId, LocalDate.now(), tokens);

        return aiResponse;
    }

    private void updateCompletedTasks(AiSession session, List<Integer> completedTaskIds) {
        Set<Integer> taskSet = new HashSet<>();
        if (session.getCompletedTasks() != null && session.getCompletedTasks().isArray()) {
            for (JsonNode idNode : session.getCompletedTasks()) {
                taskSet.add(idNode.asInt());
            }
        }
        taskSet.addAll(completedTaskIds);

        ArrayNode updatedArray = objectMapper.createArrayNode();
        taskSet.stream().sorted().forEach(updatedArray::add);
        session.setCompletedTasks(updatedArray);
        sessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public AiSessionResponse getSession(Long userId, Long sessionId) {
        AiSession session = sessionRepository.findById(sessionId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.AI_SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.AI_SESSION_FORBIDDEN);
        }

        List<AiMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return speakingMapper.toSessionResponse(session, messages);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiSessionResponse> getUserSessions(Long userId, Pageable pageable) {
        Page<AiSession> sessionPage = sessionRepository.findByUser_IdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable);
        return sessionPage.map(session -> {
            long count = messageRepository.countBySessionId(session.getId());
            return speakingMapper.toSessionSummaryResponse(session, (int) count);
        });
    }

    @Override
    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        AiSession session = sessionRepository.findById(sessionId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.AI_SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.AI_SESSION_FORBIDDEN);
        }

        session.setDeleted(true);
        session.setDeletedAt(Instant.now());
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void deleteAllSessions(Long userId) {
        sessionRepository.softDeleteAllByUserId(userId, Instant.now());
    }
}
