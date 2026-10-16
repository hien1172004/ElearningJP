package com.jp.elearningjp.shared.event.curriculum;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

/**
 * Base event cho module Curriculum (Course, Lesson, Enrollment, Progress).
 *
 * <p>Tất cả event kế thừa class này đều có {@code occurredAt} tự động = thời
 * điểm publish, dùng để truy vết / log / debug.</p>
 *
 * <p>Sau này khi cần scale đa instance, chỉ cần thay bean {@code ApplicationEventPublisher}
 * từ in-process sang Kafka (zero refactor ở producer).</p>
 */
@Getter
public abstract class CurriculumEvent extends ApplicationEvent {

    /** Thời điểm phát sinh event */
    private final long occurredAt;

    protected CurriculumEvent(Object source) {
        super(source);
        this.occurredAt = System.currentTimeMillis();
    }
}