package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.JlptLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho factory methods của {@link CourseProgressResponse}.
 */
class CourseProgressResponseTest {

    @Nested
    @DisplayName("summary factory")
    class SummaryFactory {

        @Test
        @DisplayName("Build response rút gọn với các field đúng")
        void buildsCorrectly() {
            CourseProgressResponse response = CourseProgressResponse.summary(
                    10L, "N5 Course", 8, 4,
                    new BigDecimal("50.0")
            );

            assertEquals(10L, response.getCourseId());
            assertEquals("N5 Course", response.getCourseTitle());
            assertEquals(8, response.getTotalLessons());
            assertEquals(4, response.getCompletedLessons());
            assertEquals(0, new BigDecimal("50.0").compareTo(response.getProgressPercent()));
            assertFalse(response.isCompleted(), "4/8 chưa hoàn thành");
            // Field không set → null
            assertNull(response.getCourseLevel());
            assertNull(response.getProgress());
        }

        @Test
        @DisplayName("totalLessons=0 → isCompleted=false (tránh chia 0)")
        void emptyCourse_notCompleted() {
            CourseProgressResponse response = CourseProgressResponse.summary(
                    10L, "Empty", 0, 0, BigDecimal.ZERO
            );
            assertFalse(response.isCompleted());
        }

        @Test
        @DisplayName("completed == total → isCompleted=true")
        void fullyCompleted() {
            CourseProgressResponse response = CourseProgressResponse.summary(
                    10L, "Done", 5, 5, new BigDecimal("100.0")
            );
            assertTrue(response.isCompleted());
        }
    }

    @Nested
    @DisplayName("detailed factory")
    class DetailedFactory {

        @Test
        @DisplayName("Build response đầy đủ")
        void buildsCorrectly() {
            Instant start = Instant.parse("2024-01-01T00:00:00Z");
            Instant lastAccess = Instant.parse("2024-06-15T00:00:00Z");

            CourseProgressResponse response = CourseProgressResponse.detailed(
                    10L, "N5 Course", JlptLevel.N5,
                    10, 5, 3,
                    start, lastAccess,
                    List.of(),
                    50
            );

            assertEquals(10L, response.getCourseId());
            assertEquals(JlptLevel.N5, response.getCourseLevel());
            assertEquals(10, response.getTotalLessons());
            assertEquals(5, response.getCompletedLessons());
            assertEquals(3, response.getInProgressLessons());
            assertEquals(2, response.getNotStartedLessons(), "10 - 5 - 3 = 2");
            assertEquals(0, new BigDecimal("50.0").compareTo(response.getProgressPercent()));
            assertEquals(start, response.getEnrolledAt());
            assertEquals(lastAccess, response.getLastAccessedAt());
            assertNotNull(response.getProgress());
        }

        @Test
        @DisplayName("percentResult=null → 0%")
        void nullPercent() {
            CourseProgressResponse response = CourseProgressResponse.detailed(
                    10L, "X", null, 5, 0, 0,
                    null, null, null, null
            );
            assertEquals(0, BigDecimal.ZERO.compareTo(response.getProgressPercent()));
        }

        @Test
        @DisplayName("notStarted không âm (clamp về 0)")
        void notStartedNotNegative() {
            // completed + inProgress > total (edge case: race condition)
            CourseProgressResponse response = CourseProgressResponse.detailed(
                    10L, "X", null, 5, 8, 0,
                    null, null, null, 100
            );
            assertEquals(0, response.getNotStartedLessons());
        }
    }
}
