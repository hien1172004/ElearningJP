package com.jp.elearningjp.controller.speaking;

import com.jp.elearningjp.dto.response.speaking.AiScenarioResponse;
import com.jp.elearningjp.service.speaking.AiScenarioService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.AiScenario.BASE)
@RequiredArgsConstructor
@Tag(name = "AI Speaking Scenario", description = "APIs danh sách và chi tiết các tình huống luyện nói JLPT")
public class AiScenarioController {

    private final AiScenarioService scenarioService;

    @GetMapping
    @Operation(summary = "Lấy danh sách kịch bản luyện nói (lọc theo chủ đề hoặc cấp độ JLPT)")
    public ApiResponse<List<AiScenarioResponse>> getScenarios(
            @RequestParam(required = false) String topicId,
            @RequestParam(required = false) JlptLevel jlptLevel) {

        List<AiScenarioResponse> scenarios = scenarioService.getScenarios(topicId, jlptLevel);

        return ApiResponse.<List<AiScenarioResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(scenarios)
                .build();
    }

    @GetMapping(ApiPaths.AiScenario.BY_ID)
    @Operation(summary = "Chi tiết một kịch bản luyện nói theo ID")
    public ApiResponse<AiScenarioResponse> getScenarioById(@PathVariable Long id) {
        AiScenarioResponse scenario = scenarioService.getScenarioById(id);

        return ApiResponse.<AiScenarioResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(scenario)
                .build();
    }

    @PostMapping
    @Operation(summary = "Tạo kịch bản luyện nói tùy chỉnh từ người dùng")
    public ApiResponse<AiScenarioResponse> createCustomScenario(
            @jakarta.validation.Valid @RequestBody com.jp.elearningjp.dto.request.speaking.CreateCustomScenarioRequest request) {

        AiScenarioResponse scenario = scenarioService.createCustomScenario(request);

        return ApiResponse.<AiScenarioResponse>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Tạo tình huống thành công")
                .data(scenario)
                .build();
    }
}
