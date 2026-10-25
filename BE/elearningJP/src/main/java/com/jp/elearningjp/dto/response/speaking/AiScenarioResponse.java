package com.jp.elearningjp.dto.response.speaking;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiScenarioResponse {

    private Long id;

    private String topicId;

    private JlptLevel jlptLevel;

    private String title;

    private String description;

    private String userRole;

    private String aiRole;

    private JsonNode tasks;

    private JsonNode sampleDialogue;

    private JsonNode vocabulary;

    private JsonNode grammar;

    private JsonNode tips;

    private boolean isPremium;

    private String thumbnailUrl;

    private Instant createdAt;
}
