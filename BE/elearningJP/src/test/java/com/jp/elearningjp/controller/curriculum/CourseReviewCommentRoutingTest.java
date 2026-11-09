package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.CourseReviewCommentRequest;
import com.jp.elearningjp.service.curriculum.CourseReviewCommentService;
import com.jp.elearningjp.service.curriculum.snapshot.CourseChangesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Routing + validation cho nhận xét duyệt và danh sách thay đổi. */
class CourseReviewCommentRoutingTest {

    private MockMvc mockMvc;
    private CourseReviewCommentService commentService;
    private CourseChangesService changesService;

    @BeforeEach
    void setup() {
        commentService = mock(CourseReviewCommentService.class);
        changesService = mock(CourseChangesService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseReviewCommentController(commentService, changesService)).build();
    }

    @Test
    void reads_mapped() throws Exception {
        mockMvc.perform(get("/api/v1/courses/10/pending-changes")).andExpect(status().isOk());
        verify(changesService).getChanges(10L);
        mockMvc.perform(get("/api/v1/courses/10/review-comments")).andExpect(status().isOk());
        verify(commentService).getLatestForCourse(10L);
        mockMvc.perform(get("/api/v1/course-review-requests/7/comments")).andExpect(status().isOk());
        verify(commentService).getByRequest(7L);
    }

    @Test
    void addComment_requiresContent() throws Exception {
        mockMvc.perform(post("/api/v1/course-review-requests/7/comments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lessonId\":3,\"content\":\"  \"}"))
                .andExpect(status().isBadRequest());
        verify(commentService, never()).add(anyLong(), any());

        mockMvc.perform(post("/api/v1/course-review-requests/7/comments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lessonId\":3,\"content\":\"Thiếu ví dụ\"}"))
                .andExpect(status().isOk());
        verify(commentService).add(eq(7L), any(CourseReviewCommentRequest.class));
    }

    @Test
    void deleteAndResolve_mapped() throws Exception {
        mockMvc.perform(delete("/api/v1/course-review-comments/5")).andExpect(status().isOk());
        verify(commentService).delete(5L);

        mockMvc.perform(patch("/api/v1/course-review-comments/5/resolve")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/course-review-comments/5/resolve")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"resolved\":true}"))
                .andExpect(status().isOk());
        verify(commentService).resolve(5L, true);
    }
}
