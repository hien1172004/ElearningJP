package com.jp.elearningjp.service.speaking;

import com.jp.elearningjp.dto.response.speaking.AiScenarioResponse;
import com.jp.elearningjp.shared.enums.JlptLevel;

import java.util.List;

public interface AiScenarioService {

    List<AiScenarioResponse> getScenarios(String topicId, JlptLevel jlptLevel);

    AiScenarioResponse getScenarioById(Long id);

    AiScenarioResponse createCustomScenario(com.jp.elearningjp.dto.request.speaking.CreateCustomScenarioRequest request);
}
