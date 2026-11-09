package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Optimistic locking theo version client gửi lên. */
class VersionCheckTest {

    private static Lesson lessonAtVersion(long version) {
        Lesson lesson = new Lesson();
        lesson.setVersion(version);
        return lesson;
    }

    private static Lesson lesson(long id, long version) {
        Lesson lesson = lessonAtVersion(version);
        lesson.setId(id);
        return lesson;
    }

    @Test
    void sameVersion_passes() {
        assertThatCode(() -> VersionCheck.requireVersion(lessonAtVersion(3), 3L)).doesNotThrowAnyException();
    }

    @Test
    void noVersionFromClient_skipsCheck() {
        assertThatCode(() -> VersionCheck.requireVersion(lessonAtVersion(3), null)).doesNotThrowAnyException();
    }

    @Test
    void staleVersion_conflict() {
        assertThatThrownBy(() -> VersionCheck.requireVersion(lessonAtVersion(4), 3L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);
    }

    @Test
    void reorder_allVersionsMatch_passes() {
        assertThatCode(() -> VersionCheck.requireVersions(List.of(lesson(1, 2), lesson(2, 5)), Map.of(1L, 2L, 2L, 5L)))
                .doesNotThrowAnyException();
    }

    @Test
    void reorder_oneStaleElement_conflict() {
        // Người khác vừa sắp xếp lại → version bài 2 đã tăng
        assertThatThrownBy(() -> VersionCheck.requireVersions(List.of(lesson(1, 2), lesson(2, 7)), Map.of(1L, 2L, 2L, 5L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);
    }

    @Test
    void reorder_noVersionsFromClient_skipsCheck() {
        assertThatCode(() -> VersionCheck.requireVersions(List.of(lesson(1, 2)), null)).doesNotThrowAnyException();
        assertThatCode(() -> VersionCheck.requireVersions(List.of(lesson(1, 2)), Map.of())).doesNotThrowAnyException();
    }
}
