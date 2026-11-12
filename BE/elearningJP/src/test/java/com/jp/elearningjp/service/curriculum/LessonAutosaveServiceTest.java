package com.jp.elearningjp.service.curriculum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonAutosave;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.LessonAutosaveRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Autosave: chỉ người đang được giao, giới hạn kích thước / số bản / mã form, không đi qua khóa sửa bài. */
class LessonAutosaveServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T03:00:00Z");
    private final LessonAutosaveRepository repository = mock(LessonAutosaveRepository.class);
    private final LessonRepository lessonRepository = mock(LessonRepository.class);
    private final CourseAccessGuard guard = mock(CourseAccessGuard.class);
    private final CurrentUserService currentUser = mock(CurrentUserService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private LessonAutosaveService service;
    private Course course;

    private static void assertRejected(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(expected);
    }

    @BeforeEach
    void setUp() {
        course = Course.builder().title("C").build();
        course.setId(10L);
        Lesson lesson = Lesson.builder().course(course).title("L").orderIndex(1).build();
        lesson.setId(7L);
        when(lessonRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(lesson));
        when(currentUser.getCurrentUserId()).thenReturn(5L);
        service = new LessonAutosaveService(repository, lessonRepository, guard, currentUser, objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void save_upsertsJson_withoutEditLockChecks() throws Exception {
        service.save(7L, "vocab:new:12", objectMapper.readTree("{\"meaning\":\"một\"}"));

        verify(repository).upsert(5L, 7L, "vocab:new:12", "{\"meaning\":\"một\"}", NOW);
        verify(guard).requireAssignedOrAdmin(course, ErrorCode.FORBIDDEN_LESSON_ACCESS);
        // Ghi được cả khi bài chờ duyệt / ADMIN đang sửa: không đi qua requireLessonEditable
        verify(guard, never()).requireLessonEditable(any(), any());
    }

    @Test
    void unassignedTeacher_rejected() {
        doThrow(new AppException(ErrorCode.FORBIDDEN_LESSON_ACCESS))
                .when(guard).requireAssignedOrAdmin(course, ErrorCode.FORBIDDEN_LESSON_ACCESS);

        assertRejected(() -> service.save(7L, "quiz:new", TextNode.valueOf("x")), ErrorCode.FORBIDDEN_LESSON_ACCESS);
        assertRejected(() -> service.list(7L), ErrorCode.FORBIDDEN_LESSON_ACCESS);
        verify(repository, never()).upsert(anyLong(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void invalidKey_tooLarge_tooMany_rejected() {
        assertRejected(() -> service.save(7L, "../x", TextNode.valueOf("x")), ErrorCode.LESSON_AUTOSAVE_KEY_INVALID);
        assertRejected(() -> service.save(7L, "Vocab:1", TextNode.valueOf("x")), ErrorCode.LESSON_AUTOSAVE_KEY_INVALID);

        String big = "あ".repeat(LessonAutosaveService.MAX_BYTES / 3 + 10); // 3 byte UTF-8 mỗi ký tự
        assertRejected(() -> service.save(7L, "quiz:new", TextNode.valueOf(big)), ErrorCode.LESSON_AUTOSAVE_TOO_LARGE);

        when(repository.countByUserIdAndLessonId(5L, 7L)).thenReturn((long) LessonAutosaveService.MAX_PER_LESSON);
        assertRejected(() -> service.save(7L, "quiz:new", TextNode.valueOf("x")), ErrorCode.LESSON_AUTOSAVE_TOO_MANY);
        verify(repository, never()).upsert(anyLong(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void overwriteExistingForm_allowedEvenAtLimit() {
        when(repository.countByUserIdAndLessonId(5L, 7L)).thenReturn((long) LessonAutosaveService.MAX_PER_LESSON);
        LessonAutosave existing = LessonAutosave.builder().userId(5L).lessonId(7L).formKey("quiz:new").content("{}").build();
        when(repository.findByUserIdAndLessonIdOrderByUpdatedAtDesc(5L, 7L)).thenReturn(List.of(existing));

        service.save(7L, "quiz:new", TextNode.valueOf("x"));

        verify(repository).upsert(eq(5L), eq(7L), eq("quiz:new"), eq("\"x\""), eq(NOW));
    }

    @Test
    void list_returnsParsedContent_cleanupUsesRetention() {
        LessonAutosave a = LessonAutosave.builder().userId(5L).lessonId(7L).formKey("quiz:9")
                .content("{\"questionText\":\"Câu 1\"}").build();
        when(repository.findByUserIdAndLessonIdOrderByUpdatedAtDesc(5L, 7L)).thenReturn(List.of(a));

        var list = service.list(7L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getContent().get("questionText").asText()).isEqualTo("Câu 1");

        service.cleanup();
        verify(repository).deleteOlderThan(NOW.minus(LessonAutosaveService.RETENTION));
    }
}
