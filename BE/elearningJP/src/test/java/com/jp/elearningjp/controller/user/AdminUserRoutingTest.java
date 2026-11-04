package com.jp.elearningjp.controller.user;

import com.jp.elearningjp.service.user.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Verify route/method của AdminUserController được map và validation body hoạt động (MockMvc standalone). */
class AdminUserRoutingTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminUserController(mock(AdminUserService.class)))
                .build();
    }

    private int status(RequestBuilder request) {
        try {
            return mockMvc.perform(request).andReturn().getResponse().getStatus();
        } catch (Exception e) {
            return 500; // route đã map, lỗi phát sinh do service mock
        }
    }

    @Test
    void listAndDetailRoutesExist() {
        assertThat(status(get("/api/v1/admin/users").param("status", "BANNED"))).isNotEqualTo(404);
        assertThat(status(get("/api/v1/admin/users/5"))).isNotEqualTo(404);
    }

    @Test
    void updateStatus_missingStatus_returns400() {
        assertThat(status(patch("/api/v1/admin/users/5/status")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))).isEqualTo(400);
    }

    @Test
    void updateStatus_validBody_routeExists() {
        assertThat(status(patch("/api/v1/admin/users/5/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BANNED\"}"))).isNotIn(400, 404);
    }

    @Test
    void updateRoles_missingRoles_returns400() {
        assertThat(status(put("/api/v1/admin/users/5/roles")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))).isEqualTo(400);
    }

    @Test
    void updateRoles_validBody_routeExists() {
        assertThat(status(put("/api/v1/admin/users/5/roles")
                .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"TEACHER\"]}"))).isNotIn(400, 404);
    }
}
