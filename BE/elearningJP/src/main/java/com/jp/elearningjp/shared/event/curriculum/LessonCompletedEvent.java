package com.jp.elearningjp.shared.event.curriculum;

import java.math.BigDecimal;

import lombok.Getter;

/**
 * Event phát ra khi user hoàn thành một lesson.
 *
 * <p>Consumer tương lai (chưa có trong module này):
 * <ul>
 *   <li>Gamification: cộng XP, kiểm tra badge, cập nhật streak</li>
 *   <li>LearningPath: cập nhật progress cho daily_task, recalc target_date</li>
 *   <li>Notification: gửi email "Chúc mừng hoàn thành bài X!"</li>
 *   <li>Analytics: cập nhật completion rate</li>
 * </ul>
 * </p>
 */
@Getter
public class LessonCompletedEvent extends CurriculumEvent {

    private final Long userId;
    private final Long lessonId;
    private final Long courseId;
    private final BigDecimal bestScore;        // null nếu không phải MINI_TEST
    private final boolean isMiniTest;
    private final int currentCourseProgressPercent; // % progress của course SAU khi complete

    public LessonCompletedEvent(Object source, Long userId, Long lessonId, Long courseId,
                                BigDecimal bestScore, boolean isMiniTest,
                                int currentCourseProgressPercent) {
        super(source);
        this.userId = userId;
        this.lessonId = lessonId;
        this.courseId = courseId;
        this.bestScore = bestScore;
        this.isMiniTest = isMiniTest;
        this.currentCourseProgressPercent = currentCourseProgressPercent;
    }
}