package com.jp.elearningjp.service.speaking.impl;

import com.jp.elearningjp.dto.response.speaking.AiScenarioResponse;
import com.jp.elearningjp.entity.speaking.AiScenario;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.speaking.AiScenarioRepository;
import com.jp.elearningjp.service.speaking.AiScenarioService;
import com.jp.elearningjp.service.speaking.mapper.AiSpeakingMapper;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AiScenarioServiceImpl implements AiScenarioService {

    private final AiScenarioRepository scenarioRepository;
    private final AiSpeakingMapper speakingMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public List<AiScenarioResponse> getScenarios(String topicId, JlptLevel jlptLevel) {
        List<AiScenario> scenarios;

        if (topicId != null && !topicId.isBlank() && jlptLevel != null) {
            scenarios = scenarioRepository.findByTopicIdAndJlptLevel(topicId.trim(), jlptLevel);
        } else if (topicId != null && !topicId.isBlank()) {
            scenarios = scenarioRepository.findByTopicId(topicId.trim());
        } else if (jlptLevel != null) {
            scenarios = scenarioRepository.findByJlptLevel(jlptLevel);
        } else {
            scenarios = scenarioRepository.findAllByOrderByCreatedAtDesc();
        }

        return scenarios.stream()
                .map(speakingMapper::toScenarioResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AiScenarioResponse getScenarioById(Long id) {
        AiScenario scenario = scenarioRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.AI_SCENARIO_NOT_FOUND));
        return speakingMapper.toScenarioResponse(scenario);
    }

    @Override
    @Transactional
    public AiScenarioResponse createCustomScenario(com.jp.elearningjp.dto.request.speaking.CreateCustomScenarioRequest request) {
        String title = (request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle().trim()
                : (request.getDescription().length() > 30 ? request.getDescription().substring(0, 30) + "..." : request.getDescription());

        // Tasks mặc định sinh tự động theo vai
        com.fasterxml.jackson.databind.node.ArrayNode tasks = objectMapper.createArrayNode();
        tasks.add(objectMapper.createObjectNode().put("id", 0).put("text", "Chào hỏi đối phương và giới thiệu lý do"));
        tasks.add(objectMapper.createObjectNode().put("id", 1).put("text", "Trò chuyện, trao đổi nội dung chính"));
        tasks.add(objectMapper.createObjectNode().put("id", 2).put("text", "Kết thúc cuộc trò chuyện lịch sự"));

        AiScenario scenario = AiScenario.builder()
                .topicId("COMMUNITY")
                .jlptLevel(request.getJlptLevel())
                .title(title)
                .description(request.getDescription().trim())
                .userRole(request.getUserRole().trim())
                .aiRole(request.getAiRole().trim())
                .tasks(tasks)
                .sampleDialogue(objectMapper.createArrayNode())
                .vocabulary(objectMapper.createArrayNode())
                .grammar(objectMapper.createArrayNode())
                .tips(objectMapper.createArrayNode())
                .isPremium(false)
                .thumbnailUrl("https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=600&auto=format&fit=crop&q=80")
                .build();

        scenario = scenarioRepository.save(scenario);
        log.info("Created custom scenario ID: {}, title: {}", scenario.getId(), scenario.getTitle());
        return speakingMapper.toScenarioResponse(scenario);
    }
}
