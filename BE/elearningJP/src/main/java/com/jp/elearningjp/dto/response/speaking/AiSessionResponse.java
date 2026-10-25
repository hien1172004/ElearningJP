package com.jp.elearningjp.dto.response.speaking;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.AiPracticeMode;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSessionResponse {

    private Long id;

    private Long userId;

    private AiScenarioResponse scenario;

    private AiPracticeMode mode;

    private String title;

    private JsonNode completedTasks;

    @Builder.Default
    private List<AiMessageResponse> messages = new ArrayList<>();

    private int messageCount;

    private Instant createdAt;
}
