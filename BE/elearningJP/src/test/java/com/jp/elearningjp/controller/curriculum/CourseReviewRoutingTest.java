package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.service.curriculum.CourseReviewService;
import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Routing + validation cho luồng duyệt khóa học (MockMvc standalone, service mock). */
class CourseReviewRoutingTest {

    private MockMvc mockMvc;
    private CourseReviewService service;

    @BeforeEach
    void setup() {
        service = mock(CourseReviewService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseReviewController(service)).build();
    }

    @Test
    void history_and_submit_mapped() throws Exception {
        mockMvc.perform(get("/api/v1/courses/10/review-requests")).andExpect(status().isOk());
        verify(service).getHistory(10L);

        mockMvc.perform(post("/api/v1/courses/10/review-requests")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"xong\"}"))
                .andExpect(status().isOk());
        verify(service).submit(10L, "xong");
    }

    @Test
    void submit_withoutBody_allowed() throws Exception {
        mockMvc.perform(post("/api/v1/courses/10/review-requests")).andExpect(status().isOk());
        verify(service).submit(10L, null);
    }

    @Test
    void withdraw_mapped() throws Exception {
        mockMvc.perform(post("/api/v1/courses/10/review-requests/withdraw")).andExpect(status().isOk());
        verify(service).withdraw(10L);
    }

    @Test
    void queue_defaultsToPendingQueryParamOptional() throws Exception {
        mockMvc.perform(get("/api/v1/course-review-requests")).andExpect(status().isOk());
        verify(service).getQueue(isNull(), isNull(), any(Integer.class), any(Integer.class));

        mockMvc.perform(get("/api/v1/course-review-requests").param("status", "APPROVED")).andExpect(status().isOk());
        verify(service).getQueue(ReviewRequestStatus.APPROVED, null, 1, 10);

        mockMvc.perform(get("/api/v1/course-review-requests").param("keyword", "N5")).andExpect(status().isOk());
        verify(service).getQueue(null, "N5", 1, 10);
    }

    @Test
    void approve_mapped() throws Exception {
        mockMvc.perform(post("/api/v1/course-review-requests/7/approve")).andExpect(status().isOk());
        verify(service).approve(7L, null);
    }

    @Test
    void reject_requiresReason() throws Exception {
        mockMvc.perform(post("/api/v1/course-review-requests/7/reject")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).reject(anyLong(), any());

        mockMvc.perform(post("/api/v1/course-review-requests/7/reject")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Thiếu ví dụ\"}"))
                .andExpect(status().isOk());
        verify(service).reject(7L, "Thiếu ví dụ");
    }
}
