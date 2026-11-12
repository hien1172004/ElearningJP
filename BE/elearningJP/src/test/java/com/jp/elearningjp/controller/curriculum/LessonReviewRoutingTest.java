package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonReviewSubmitRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonReviewSubmitResponse;
import com.jp.elearningjp.exception.GlobalExceptionHandle;
import com.jp.elearningjp.service.curriculum.LessonReviewService;
import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Routing + validation cho duyệt theo bài (MockMvc standalone, service mock). */
class LessonReviewRoutingTest {

    private MockMvc mockMvc;
    private LessonReviewService service;

    @BeforeEach
    void setup() {
        service = mock(LessonReviewService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LessonReviewController(service))
                .setControllerAdvice(new GlobalExceptionHandle())
                .build();
    }

    @Test
    void states_and_history_mapped() throws Exception {
        mockMvc.perform(get("/api/v1/courses/10/lesson-review-states")).andExpect(status().isOk());
        verify(service).getStates(10L);

        mockMvc.perform(get("/api/v1/courses/10/lesson-reviews")).andExpect(status().isOk());
        verify(service).getHistory(10L, 1, 5); // mặc định trang 1, 5 mục

        mockMvc.perform(get("/api/v1/courses/10/lesson-reviews").param("page", "2").param("size", "8"))
                .andExpect(status().isOk());
        verify(service).getHistory(10L, 2, 8);
    }

    @Test
    @SuppressWarnings("unchecked")
    void submit_mapsLessonsWithRevisions_messageCountsPartialFailure() throws Exception {
        when(service.submit(eq(10L), any(), any())).thenReturn(
                LessonReviewSubmitResponse.builder().submittedCount(1).failedCount(1).results(List.of()).build());

        mockMvc.perform(post("/api/v1/courses/10/lesson-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lessons\":[{\"lessonId\":1,\"draftRevision\":3},"
                                + "{\"lessonId\":2,\"draftRevision\":0}],\"note\":\"xong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã gửi 1/2 bài — xem lý do từng bài chưa gửi được"));

        ArgumentCaptor<List<LessonReviewSubmitRequest.Item>> items = ArgumentCaptor.forClass(List.class);
        verify(service).submit(eq(10L), items.capture(), eq("xong"));
        assertThat(items.getValue()).extracting(LessonReviewSubmitRequest.Item::getLessonId,
                        LessonReviewSubmitRequest.Item::getDraftRevision)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1L, 3L), org.assertj.core.groups.Tuple.tuple(2L, 0L));
    }

    @Test
    void submit_emptyOrMissingRevision_rejected400() throws Exception {
        mockMvc.perform(post("/api/v1/courses/10/lesson-reviews")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lessons\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/courses/10/lesson-reviews")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lessons\":[{\"lessonId\":1}]}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).submit(anyLong(), any(), any());
    }

    @Test
    void withdraw_and_saveToLive_mapped() throws Exception {
        mockMvc.perform(post("/api/v1/lessons/5/review/withdraw")).andExpect(status().isOk());
        verify(service).withdraw(5L);

        mockMvc.perform(post("/api/v1/lessons/5/review/save")).andExpect(status().isOk());
        verify(service).saveLesson(5L, false);

        mockMvc.perform(post("/api/v1/lessons/5/review/save").param("majorChange", "true")).andExpect(status().isOk());
        verify(service).saveLesson(5L, true);

        mockMvc.perform(post("/api/v1/lessons/5/review/discard-draft")).andExpect(status().isOk());
        verify(service).restartDraft(5L, false);

        mockMvc.perform(post("/api/v1/lessons/5/draft/rebase")).andExpect(status().isOk());
        verify(service).restartDraft(5L, true);

        mockMvc.perform(get("/api/v1/lessons/5/draft/archived")).andExpect(status().isOk());
        verify(service).getArchivedDraft(5L);
    }

    @Test
    void queue_approve_reject_mapped_rejectRequiresReason() throws Exception {
        mockMvc.perform(get("/api/v1/lesson-review-requests").param("status", "APPROVED"))
                .andExpect(status().isOk());
        verify(service).getQueue(ReviewRequestStatus.APPROVED, null, 1, 20);

        mockMvc.perform(get("/api/v1/lesson-review-requests").param("keyword", "Bài 4"))
                .andExpect(status().isOk());
        verify(service).getQueue(null, "Bài 4", 1, 20);

        mockMvc.perform(post("/api/v1/lesson-review-requests/7/approve")).andExpect(status().isOk());
        verify(service).approve(7L, null, false);

        mockMvc.perform(post("/api/v1/lesson-review-requests/8/approve")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Tốt\",\"majorChange\":true}"))
                .andExpect(status().isOk());
        verify(service).approve(8L, "Tốt", true);

        mockMvc.perform(post("/api/v1/lesson-review-requests/7/reject")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).reject(anyLong(), any());

        mockMvc.perform(post("/api/v1/lesson-review-requests/7/reject")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Thiếu ví dụ\"}"))
                .andExpect(status().isOk());
        verify(service).reject(7L, "Thiếu ví dụ");
    }
}
