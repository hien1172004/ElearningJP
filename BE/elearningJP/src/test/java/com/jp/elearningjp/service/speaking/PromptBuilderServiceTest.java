package com.jp.elearningjp.service.speaking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jp.elearningjp.entity.speaking.AiMessage;
import com.jp.elearningjp.entity.speaking.AiScenario;
import com.jp.elearningjp.entity.speaking.AiSession;
import com.jp.elearningjp.shared.enums.AiMessageRole;
import com.jp.elearningjp.shared.enums.AiPracticeMode;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderServiceTest {

    private PromptBuilderService promptBuilderService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        promptBuilderService = new PromptBuilderService(objectMapper);
    }

    @Test
    @DisplayName("Build prompt cho kịch bản luyện nói (Scenario mode) thành công")
    void testBuildPrompt_ScenarioMode() throws Exception {
        ArrayNode tasks = objectMapper.createArrayNode();
        tasks.addObject().put("id", 1).put("text", "Chào hỏi nhân viên");
        tasks.addObject().put("id", 2).put("text", "Gọi món mì Ramen");

        ArrayNode completedTasks = objectMapper.createArrayNode();
        completedTasks.add(1);

        AiScenario scenario = AiScenario.builder()
                .topicId("restaurant")
                .jlptLevel(JlptLevel.N5)
                .title("Gọi món ở quán Ramen")
                .description("Bạn vào quán ramen và gọi món")
                .userRole("Khách hàng")
                .aiRole("Nhân viên quán")
                .tasks(tasks)
                .build();

        AiSession session = AiSession.builder()
                .mode(AiPracticeMode.SCENARIO)
                .scenario(scenario)
                .completedTasks(completedTasks)
                .title("Gọi món ở quán Ramen")
                .build();

        AiMessage m1 = AiMessage.builder().role(AiMessageRole.AI).textJa("いらっしゃいませ！").build();
        AiMessage m2 = AiMessage.builder().role(AiMessageRole.USER).textJa("こんにちは。").build();

        LlmSpeakingPrompt prompt = promptBuilderService.buildPrompt(session, List.of(m1, m2), "ラーメンをください。");

        assertNotNull(prompt);
        assertEquals("ラーメンをください。", prompt.getUserNewMessage());
        assertEquals(2, prompt.getRecentHistory().size());

        String sys = prompt.getSystemInstruction();
        assertTrue(sys.contains("Gọi món ở quán Ramen"));
        assertTrue(sys.contains("Nhân viên quán"));
        assertTrue(sys.contains("Khách hàng"));
        assertTrue(sys.contains("[Task ID 1]: Chào hỏi nhân viên (ALREADY COMPLETED)"));
        assertTrue(sys.contains("[Task ID 2]: Gọi món mì Ramen (NOT COMPLETED YET)"));
        assertTrue(sys.contains("completed_task_ids"));
        assertTrue(sys.contains("reply_ja"));
    }

    @Test
    @DisplayName("Build prompt cho trò chuyện tự do (Free talk mode) thành công")
    void testBuildPrompt_FreeTalkMode() {
        AiSession session = AiSession.builder()
                .mode(AiPracticeMode.FREE)
                .title("好きなアニメ")
                .completedTasks(objectMapper.createArrayNode())
                .build();

        LlmSpeakingPrompt prompt = promptBuilderService.buildPrompt(session, List.of(), "こんにちは！アニメが好きです。");

        assertNotNull(prompt);
        assertEquals("こんにちは！アニメが好きです。", prompt.getUserNewMessage());

        String sys = prompt.getSystemInstruction();
        assertTrue(sys.contains("Maziiko"));
        assertTrue(sys.contains("好きなアニメ"));
        assertTrue(sys.contains("reply_ja"));
    }
}
