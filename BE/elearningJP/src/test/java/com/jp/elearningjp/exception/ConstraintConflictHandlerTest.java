package com.jp.elearningjp.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hai thao tác song song cùng vượt bước kiểm tra ở service rồi vi phạm UNIQUE ở DB →
 * trả mã nghiệp vụ 409 (như khi bị chặn tuần tự), không trả 500.
 * Và AppException kèm message cụ thể → trả đúng message đó.
 */
class ConstraintConflictHandlerTest {

    @RestController
    static class ConflictController {
        @GetMapping("/assign-twice")
        public String assignTwice() {
            throw violation("duplicate key value violates unique constraint \"uq_course_teachers_course_teacher\"");
        }

        @GetMapping("/submit-twice")
        public String submitTwice() {
            throw violation("duplicate key value violates unique constraint \"uq_course_review_requests_pending\"");
        }

        @GetMapping("/unknown")
        public String unknown() {
            throw violation("violates foreign key constraint \"fk_something\"");
        }

        @GetMapping("/detail")
        public String detail() {
            throw new AppException(ErrorCode.COURSE_REVIEW_NOT_PENDING, "Đã được U1 duyệt lúc 10:32");
        }

        private static DataIntegrityViolationException violation(String dbMessage) {
            return new DataIntegrityViolationException("could not execute statement", new SQLException(dbMessage));
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ConflictController())
            .setControllerAdvice(new GlobalExceptionHandle())
            .build();

    @Test
    void duplicateTeacherAssignment_mapsTo409AlreadyAssigned() throws Exception {
        mockMvc.perform(get("/assign-twice"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.COURSE_TEACHER_ALREADY_ASSIGNED.getCode()));
    }

    @Test
    void secondPendingReviewRequest_mapsTo409AlreadyPending() throws Exception {
        mockMvc.perform(get("/submit-twice"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.COURSE_REVIEW_ALREADY_PENDING.getCode()));
    }

    @Test
    void unknownConstraint_staysUncaught500() throws Exception {
        mockMvc.perform(get("/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNCAUGHT_EXCEPTION.getCode()));
    }

    @Test
    void appExceptionWithDetail_returnsDetailMessage() throws Exception {
        mockMvc.perform(get("/detail"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.COURSE_REVIEW_NOT_PENDING.getCode()))
                .andExpect(jsonPath("$.message").value("Đã được U1 duyệt lúc 10:32"));
    }
}
