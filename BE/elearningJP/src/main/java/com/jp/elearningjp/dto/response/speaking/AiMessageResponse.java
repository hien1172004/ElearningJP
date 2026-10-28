package com.jp.elearningjp.dto.response.speaking;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.AiMessageRole;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMessageResponse {

    private Long id;

    private Long sessionId;

    private AiMessageRole role; // USER hoặc AI

    private String textJa;

    private String textVi;

    private String audioUrl;

    private JsonNode correction;

    private Instant createdAt;
}
