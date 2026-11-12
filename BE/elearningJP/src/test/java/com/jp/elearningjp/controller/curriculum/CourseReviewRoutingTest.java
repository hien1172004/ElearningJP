package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.service.curriculum.CourseReviewService;
import com.jp.elearningjp.service.curriculum.snapshot.CourseChangesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Routing phiên bản cả khóa; luồng gửi / duyệt cả khóa đã bỏ (MockMvc standalone, service mock). */
class CourseReviewRoutingTest {

    private MockMvc mockMvc;
    private CourseReviewService service;
    private CourseChangesService changesService;

    @BeforeEach
    void setup() {
        service = mock(CourseReviewService.class);
        changesService = mock(CourseChangesService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseReviewController(service, changesService)).build();
    }

    /** Hộp "Thay đổi chưa lưu" + xác nhận Lưu dùng route này — từng bị mất khi bỏ luồng duyệt cả khóa. */
    @Test
    void pendingChanges_mapped() throws Exception {
        mockMvc.perform(get("/api/v1/courses/10/pending-changes")).andExpect(status().isOk());
        verify(changesService).getChanges(10L);
    }

    @Test
    void saveVersion_mapped() throws Exception {
        mockMvc.perform(post("/api/v1/courses/10/versions")).andExpect(status().isOk());
        verify(service).saveVersion(10L);
    }

    @Test
    void wholeCourseReviewEndpoints_removed() throws Exception {
        mockMvc.perform(post("/api/v1/courses/10/review-requests")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/courses/10/review-requests")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/courses/10/review-requests/withdraw")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/course-review-requests")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/course-review-requests/5/approve")).andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }
}
