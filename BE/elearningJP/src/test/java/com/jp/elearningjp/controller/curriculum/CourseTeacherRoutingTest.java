package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.response.curriculum.CourseTeacherResponse;
import com.jp.elearningjp.service.curriculum.CourseTeacherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Routing + validation cho API phân công giáo viên (MockMvc standalone, service mock). */
class CourseTeacherRoutingTest {

    private MockMvc mockMvc;
    private CourseTeacherService service;

    @BeforeEach
    void setup() {
        service = mock(CourseTeacherService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseTeacherController(service)).build();
    }

    @Test
    void getTeachers_mapped() throws Exception {
        when(service.getTeachers(10L)).thenReturn(List.of(CourseTeacherResponse.builder().teacherId(5L).build()));

        mockMvc.perform(get("/api/v1/courses/10/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].teacherId").value(5));
    }

    @Test
    void assignTeacher_mapped() throws Exception {
        mockMvc.perform(post("/api/v1/courses/10/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":5}"))
                .andExpect(status().isOk());

        verify(service).assignTeacher(10L, 5L);
    }

    @Test
    void assignTeacher_missingTeacherId_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/courses/10/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).assignTeacher(anyLong(), anyLong());
    }

    @Test
    void unassignTeacher_mapped() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/10/teachers/5"))
                .andExpect(status().isOk());

        verify(service).unassignTeacher(eq(10L), eq(5L));
    }
}
