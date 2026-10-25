package com.jp.elearningjp.service.speaking.mapper;

import com.jp.elearningjp.dto.response.speaking.AiMessageResponse;
import com.jp.elearningjp.dto.response.speaking.AiScenarioResponse;
import com.jp.elearningjp.dto.response.speaking.AiSessionResponse;
import com.jp.elearningjp.entity.speaking.AiMessage;
import com.jp.elearningjp.entity.speaking.AiScenario;
import com.jp.elearningjp.entity.speaking.AiSession;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AiSpeakingMapper {

    public AiScenarioResponse toScenarioResponse(AiScenario scenario) {
        if (scenario == null) return null;
        return AiScenarioResponse.builder()
                .id(scenario.getId())
                .topicId(scenario.getTopicId())
                .jlptLevel(scenario.getJlptLevel())
                .title(scenario.getTitle())
                .description(scenario.getDescription())
                .userRole(scenario.getUserRole())
                .aiRole(scenario.getAiRole())
                .tasks(scenario.getTasks())
                .sampleDialogue(scenario.getSampleDialogue())
                .vocabulary(scenario.getVocabulary())
                .grammar(scenario.getGrammar())
                .tips(scenario.getTips())
                .isPremium(scenario.isPremium())
                .thumbnailUrl(scenario.getThumbnailUrl())
                .createdAt(scenario.getCreatedAt())
                .build();
    }

    public AiMessageResponse toMessageResponse(AiMessage message) {
        if (message == null) return null;
        return AiMessageResponse.builder()
                .id(message.getId())
                .sessionId(message.getSession() != null ? message.getSession().getId() : null)
                .role(message.getRole())
                .textJa(message.getTextJa())
                .textVi(message.getTextVi())
                .audioUrl(message.getAudioUrl())
                .correction(message.getCorrection())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public List<AiMessageResponse> toMessageResponses(List<AiMessage> messages) {
        if (messages == null || messages.isEmpty()) return Collections.emptyList();
        return messages.stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    public AiSessionResponse toSessionResponse(AiSession session, List<AiMessage> messages) {
        if (session == null) return null;
        List<AiMessageResponse> messageDtos = toMessageResponses(messages);
        return AiSessionResponse.builder()
                .id(session.getId())
                .userId(session.getUser() != null ? session.getUser().getId() : null)
                .scenario(toScenarioResponse(session.getScenario()))
                .mode(session.getMode())
                .title(session.getTitle())
                .completedTasks(session.getCompletedTasks())
                .messages(messageDtos)
                .messageCount(messageDtos.size())
                .createdAt(session.getCreatedAt())
                .build();
    }

    public AiSessionResponse toSessionSummaryResponse(AiSession session, int messageCount) {
        if (session == null) return null;
        return AiSessionResponse.builder()
                .id(session.getId())
                .userId(session.getUser() != null ? session.getUser().getId() : null)
                .scenario(toScenarioResponse(session.getScenario()))
                .mode(session.getMode())
                .title(session.getTitle())
                .completedTasks(session.getCompletedTasks())
                .messages(Collections.emptyList())
                .messageCount(messageCount)
                .createdAt(session.getCreatedAt())
                .build();
    }
}
