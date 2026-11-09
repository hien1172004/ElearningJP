package com.jp.elearningjp.exception;

import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Xung đột optimistic lock (2 giao dịch sửa chồng nhau) → 409 CONCURRENT_MODIFICATION thay vì 500. */
class OptimisticLockHandlerTest {

    @RestController
    static class ConflictController {
        @GetMapping("/conflict")
        public String conflict() {
            throw new ObjectOptimisticLockingFailureException("Lesson", 1L);
        }

        @GetMapping("/stale")
        public String stale() {
            throw new AppException(ErrorCode.CONCURRENT_MODIFICATION);
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ConflictController())
            .setControllerAdvice(new GlobalExceptionHandle())
            .build();

    @Test
    void jpaOptimisticLock_mapsTo409() throws Exception {
        mockMvc.perform(get("/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONCURRENT_MODIFICATION.getCode()));
    }

    @Test
    void staleClientVersion_mapsTo409() throws Exception {
        mockMvc.perform(get("/stale"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONCURRENT_MODIFICATION.getCode()));
    }
}
