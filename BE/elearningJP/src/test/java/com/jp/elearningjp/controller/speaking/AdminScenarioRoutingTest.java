package com.jp.elearningjp.controller.speaking;

import com.jp.elearningjp.service.speaking.AdminScenarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Verify route/method của AdminScenarioController và validation body (MockMvc standalone). */
class AdminScenarioRoutingTest {

    private static final String VALID_BODY = """
            {"topicId":"DINING_SHOPPING","jlptLevel":"N5","title":"T","description":"D",
             "userRole":"Khách","aiRole":"Nhân viên","tasks":[{"text":"Chào"}],
             "sampleDialogue":[{"speaker":"AI","text":"こんにちは"}]}
            """;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminScenarioController(mock(AdminScenarioService.class)))
                .build();
    }

    private int status(RequestBuilder request) {
        try {
            return mockMvc.perform(request).andReturn().getResponse().getStatus();
        } catch (Exception e) {
            return 500; // route đã map, lỗi do service mock
        }
    }

    @Test
    void readRoutesExist() {
        assertThat(status(get("/api/v1/admin/scenarios").param("jlptLevel", "N5"))).isNotEqualTo(404);
        assertThat(status(get("/api/v1/admin/scenarios/3"))).isNotEqualTo(404);
        assertThat(status(delete("/api/v1/admin/scenarios/3"))).isNotEqualTo(404);
    }

    @Test
    void create_validBody_passesValidation() {
        assertThat(status(post("/api/v1/admin/scenarios").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)))
                .isNotIn(400, 404);
    }

    @Test
    void create_withoutTasks_returns400() {
        String body = VALID_BODY.replace("\"tasks\":[{\"text\":\"Chào\"}],", "\"tasks\":[],");
        assertThat(status(post("/api/v1/admin/scenarios").contentType(MediaType.APPLICATION_JSON).content(body)))
                .isEqualTo(400);
    }

    @Test
    void update_invalidSpeaker_returns400() {
        String body = VALID_BODY.replace("\"speaker\":\"AI\"", "\"speaker\":\"BOT\"");
        assertThat(status(put("/api/v1/admin/scenarios/3").contentType(MediaType.APPLICATION_JSON).content(body)))
                .isEqualTo(400);
    }

    @Test
    void create_invalidTopicId_returns400() {
        String body = VALID_BODY.replace("DINING_SHOPPING", "dining shopping");
        assertThat(status(post("/api/v1/admin/scenarios").contentType(MediaType.APPLICATION_JSON).content(body)))
                .isEqualTo(400);
    }
}
